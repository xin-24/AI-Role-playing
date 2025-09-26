package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.QiniuAsrService;
import com.ai.roleplay.service.QiniuAIService;
import com.ai.roleplay.service.QiniuTtsService;

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
            com.ai.roleplay.model.Character character = characterRepository.findById(characterId).orElse(null);
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
            String aiResponse = qiniuAIService.generateAIResponse(
                    character.getName(),
                    character.getDescription(),
                    character.getPersonalityTraits(),
                    character.getBackgroundStory(),
                    historyData);

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

            // 8. 生成TTS音频数据
            try {
                byte[] audioBytes = qiniuTtsService.synthesize(aiResponse, character.getVoiceType(), "mp3");
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                response.put("audioData", base64Audio);
                response.put("audioFormat", "mp3");
            } catch (Exception e) {
                // 如果TTS生成失败，不中断主要流程
                response.put("audioError", "语音生成失败: " + e.getMessage());
            }

            // 9. 构建成功响应
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
            com.ai.roleplay.model.Character character = characterRepository.findById(characterId).orElse(null);
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
            String aiResponse = qiniuAIService.generateAIResponse(
                    character.getName(),
                    character.getDescription(),
                    character.getPersonalityTraits(),
                    character.getBackgroundStory(),
                    historyData);

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

            // 8. 生成TTS音频数据
            try {
                byte[] audioBytes = qiniuTtsService.synthesize(aiResponse, character.getVoiceType(), "mp3");
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                response.put("audioData", base64Audio);
                response.put("audioFormat", "mp3");
            } catch (Exception e) {
                // 如果TTS生成失败，不中断主要流程
                response.put("audioError", "语音生成失败: " + e.getMessage());
            }

            // 9. 构建成功响应
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

    // 按照标点符号（。！？）分割文本
    private List<String> splitByPunctuation(String text) {
        List<String> segments = new ArrayList<>();
        Pattern pattern = Pattern.compile("([^。！？]*[。！？])");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            segments.add(matcher.group(1));
        }
        
        // 处理最后可能剩余的部分
        String[] parts = pattern.split(text);
        if (parts.length > 0 && !parts[parts.length - 1].trim().isEmpty()) {
            segments.add(parts[parts.length - 1].trim());
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