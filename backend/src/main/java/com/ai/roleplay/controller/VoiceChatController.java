package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.QiniuAsrService;
import com.ai.roleplay.service.QiniuAIService;
import com.ai.roleplay.service.QiniuTtsService;
import com.ai.roleplay.service.CharacterPromptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voice-chat")
@CrossOrigin(origins = "*")
public class VoiceChatController {

    @Autowired
    private QiniuAsrService qiniuAsrService;

    @Autowired
    private QiniuAIService qiniuAIService;

    @Autowired
    private QiniuTtsService qiniuTtsService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private CharacterPromptService characterPromptService;

    @Value("${server.port:8082}")
    private String serverPort;

    /**
     * 专门用于ASR测试的端点 - 使用data字段方式
     */
    @PostMapping(value = "/api/asr/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribeAudioForTest(@RequestParam("file") MultipartFile file) {
        try {
            // 使用data字段方式直接处理音频文件
            return qiniuAsrService.transcribe(file);
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }

    /**
     * 专门用于ASR测试的端点 - 使用URL方式
     */
    @GetMapping("/api/asr/transcribe")
    public String transcribeAudioUrlForTest(@RequestParam("url") String audioUrl) {
        try {
            // 使用URL方式处理音频文件
            return qiniuAsrService.transcribeByUrl(audioUrl);
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }

    /**
     * 处理语音输入，转换为文本并获取AI回复
     */
    @PostMapping(value = "/send-voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> sendVoiceMessage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("characterId") Long characterId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 使用ASR服务将语音转换为文本 (优先使用data字段方式)
            String transcribedText = transcribeWithUrlStrategy(file);

            if (transcribedText == null || transcribedText.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "语音转文本失败，未识别到有效内容");
                return response;
            }

            response.put("transcribedText", transcribedText);

            // 2. 保存用户语音转换的文本消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setCharacterId(characterId);
            userMessage.setMessage(transcribedText);
            userMessage.setIsUserMessage(true);
            ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

            // 3. 获取角色信息
            com.ai.roleplay.model.Character character = null;
            if (characterId < 0) {
                // 处理硬编码角色
                character = getHardcodedCharacter(characterId);
            } else {
                // 处理数据库中的角色
                character = characterRepository.findById(characterId).orElse(null);
            }
            
            if (character == null) {
                response.put("success", false);
                response.put("error", "未找到指定角色");
                return response;
            }

            // 4. 获取对话历史
            List<ChatMessage> chatHistory = chatMessageRepository
                    .findByCharacterIdOrderByCreatedAtAsc(characterId);

            // 5. 准备对话历史数据
            List<Map<String, Object>> historyData = chatHistory.stream().map(msg -> {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("isUserMessage", msg.getIsUserMessage());
                messageMap.put("message", msg.getMessage());
                return messageMap;
            }).collect(Collectors.toList());

            // 6. 生成AI回复
            String aiResponse = null;
            String characterName = character.getName();
            if (characterPromptService.hasCharacterPrompt(characterName)) {
                // 对于硬编码角色，使用CharacterPromptService中的提示词
                aiResponse = qiniuAIService.generateAIResponse(
                        character.getName(),
                        "", // 描述为空
                        "", // 性格特征为空
                        "", // 背景故事为空
                        historyData);
            } else {
                // 对于数据库中的角色，使用原有的方式
                aiResponse = qiniuAIService.generateAIResponse(
                        character.getName(),
                        character.getDescription(),
                        character.getPersonalityTraits(),
                        character.getBackgroundStory(),
                        historyData);
            }

            // 7. 按照标点符号（。！？）分割AI回复
            List<String> aiResponseSegments = splitByPunctuation(aiResponse);

            // 保存分割后的AI回复消息
            List<ChatMessage> savedAiMessages = new ArrayList<>();
            for (String segment : aiResponseSegments) {
                if (!segment.trim().isEmpty()) {
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setCharacterId(characterId);
                    aiMessage.setMessage(segment.trim());
                    aiMessage.setIsUserMessage(false);
                    ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                    savedAiMessages.add(savedAiMessage);
                }
            }

            // 8. 构建成功响应
            response.put("success", true);
            response.put("userMessage", savedUserMessage);
            response.put("aiMessages", savedAiMessages);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "处理语音消息时发生错误: " + e.getMessage());
        }

        return response;
    }

    /**
     * 使用URL直接处理语音输入，转换为文本并获取AI回复
     * 这是一个额外的接口，直接使用公网URL进行ASR转录
     */
    @GetMapping("/send-voice-url")
    public Map<String, Object> sendVoiceMessageByUrl(
            @RequestParam("audioUrl") String audioUrl,
            @RequestParam("characterId") Long characterId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 使用ASR服务将语音转换为文本 (直接使用URL方式)
            String transcribedText = qiniuAsrService.transcribeByUrl(audioUrl);

            if (transcribedText == null || transcribedText.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "语音转文本失败，未识别到有效内容");
                return response;
            }

            response.put("transcribedText", transcribedText);

            // 2. 保存用户语音转换的文本消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setCharacterId(characterId);
            userMessage.setMessage(transcribedText);
            userMessage.setIsUserMessage(true);
            ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

            // 3. 获取角色信息
            com.ai.roleplay.model.Character character = null;
            if (characterId < 0) {
                // 处理硬编码角色
                character = getHardcodedCharacter(characterId);
            } else {
                // 处理数据库中的角色
                character = characterRepository.findById(characterId).orElse(null);
            }
            
            if (character == null) {
                response.put("success", false);
                response.put("error", "未找到指定角色");
                return response;
            }

            // 4. 获取对话历史
            List<ChatMessage> chatHistory = chatMessageRepository
                    .findByCharacterIdOrderByCreatedAtAsc(characterId);

            // 5. 准备对话历史数据
            List<Map<String, Object>> historyData = chatHistory.stream().map(msg -> {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("isUserMessage", msg.getIsUserMessage());
                messageMap.put("message", msg.getMessage());
                return messageMap;
            }).collect(Collectors.toList());

            // 6. 生成AI回复
            String aiResponse = null;
            String characterName = character.getName();
            if (characterPromptService.hasCharacterPrompt(characterName)) {
                // 对于硬编码角色，使用CharacterPromptService中的提示词
                aiResponse = qiniuAIService.generateAIResponse(
                        character.getName(),
                        "", // 描述为空
                        "", // 性格特征为空
                        "", // 背景故事为空
                        historyData);
            } else {
                // 对于数据库中的角色，使用原有的方式
                aiResponse = qiniuAIService.generateAIResponse(
                        character.getName(),
                        character.getDescription(),
                        character.getPersonalityTraits(),
                        character.getBackgroundStory(),
                        historyData);
            }

            // 7. 按照标点符号（。！？）分割AI回复
            List<String> aiResponseSegments = splitByPunctuation(aiResponse);

            // 保存分割后的AI回复消息
            List<ChatMessage> savedAiMessages = new ArrayList<>();
            for (String segment : aiResponseSegments) {
                if (!segment.trim().isEmpty()) {
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setCharacterId(characterId);
                    aiMessage.setMessage(segment.trim());
                    aiMessage.setIsUserMessage(false);
                    ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                    savedAiMessages.add(savedAiMessage);
                }
            }

            // 8. 构建成功响应
            response.put("success", true);
            response.put("userMessage", savedUserMessage);
            response.put("aiMessages", savedAiMessages);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "处理语音消息时发生错误: " + e.getMessage());
        }

        return response;
    }
    
    // 获取硬编码角色
    private com.ai.roleplay.model.Character getHardcodedCharacter(Long characterId) {
        com.ai.roleplay.model.Character character = new com.ai.roleplay.model.Character();
        character.setId(characterId);
        
        switch (characterId.intValue()) {
            case -1:
                character.setName("哈利·波特");
                character.setDescription("霍格沃茨魔法学校的学生");
                character.setPersonalityTraits("勇敢、正直、有正义感、略带腼腆");
                character.setBackgroundStory("生活在霍格沃茨魔法学校，与朋友们一起对抗黑魔法师");
                character.setVoiceType("qiniu_zh_male_ljfdxz");
                character.setIsDeletable(false);
                break;
            case -2:
                character.setName("苏格拉底");
                character.setDescription("古希腊哲学家，被誉为西方哲学的奠基人");
                character.setPersonalityTraits("智慧、善于提问、谦逊、追求真理");
                character.setBackgroundStory("生活在古希腊，通过对话和提问来探索真理");
                character.setVoiceType("qiniu_zh_male_ybxknjs");
                character.setIsDeletable(false);
                break;
            case -3:
                character.setName("音乐老师");
                character.setDescription("经验丰富的音乐教育工作者");
                character.setPersonalityTraits("耐心、热情、严谨、富有创造力");
                character.setBackgroundStory("拥有丰富的音乐理论和实践经验，致力于音乐教育");
                character.setVoiceType("qiniu_zh_female_zxjxnjs");
                character.setIsDeletable(false);
                break;
            default:
                return null;
        }
        
        return character;
    }

    /**
     * 使用策略进行语音转文本
     * 只使用data字段方式处理语音转文本
     */
    private String transcribeWithUrlStrategy(MultipartFile file) {
        try {
            // 直接使用data字段方式处理文件（更稳定可靠）
            return qiniuAsrService.transcribe(file);
        } catch (Exception e) {
            // 如果data字段方式失败，抛出异常
            throw new RuntimeException("ASR转录失败: " + e.getMessage());
        }
    }

    // 按照标点符号（。！？）分割文本，但忽略引号内的标点符号
    private List<String> splitByPunctuation(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        
        // 使用正则表达式按标点符号分割，但不拆分引号内的内容
        // 这个正则表达式会匹配引号外的标点符号
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // 检查引号的开始和结束
            if ((c == '"' || c == '“' || c == '”' || c == '\'' || c == '‘' || c == '’') && 
                (i == 0 || text.charAt(i-1) != '\\')) { // 忽略转义的引号
                if (!inQuotes) {
                    // 开始引号
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar || 
                          (quoteChar == '“' && c == '”') || 
                          (quoteChar == '”' && c == '“') ||
                          (quoteChar == '\'' && c == '\'') ||
                          (quoteChar == '‘' && c == '’') ||
                          (quoteChar == '’' && c == '‘')) {
                    // 结束引号
                    inQuotes = false;
                    quoteChar = 0;
                }
            }
            
            // 如果遇到标点符号且不在引号内，则分割
            if ((c == '。' || c == '！' || c == '？'  ||c =='"'|| c == '!' || c == '?') && !inQuotes) {
                currentPart.append(c);
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            } else {
                currentPart.append(c);
            }
        }
        
        // 添加最后一部分
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }
        
        // 清理和添加到最终结果
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        
        // 如果没有匹配到任何标点符号，则将整个文本作为一个片段
        if (segments.isEmpty()) {
            segments.add(text.trim());
        }
        
        return segments;
    }

    /**
     * 保存文件并返回可访问的URL
     */
    private String saveFileAndGetUrl(MultipartFile file) throws IOException {
        // 创建临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        Path uploadDir = Paths.get(tempDir, "voice_uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.write(filePath, file.getBytes());

        // 返回文件的URL（在实际生产环境中，这里应该是公网可访问的URL）
        // 这里为了测试，我们返回本地文件路径的URL格式
        String fileUrl = String.format("http://localhost:%s/temp/%s", serverPort, uniqueFilename);

        // 注意：在实际应用中，你需要将文件上传到OSS/CDN等可公网访问的地方
        // 这里只是一个临时的解决方案用于测试

        return fileUrl;
    }
}