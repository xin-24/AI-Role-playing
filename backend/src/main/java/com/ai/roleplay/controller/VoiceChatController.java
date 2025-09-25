package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.QiniuAsrService;
import com.ai.roleplay.service.QiniuAIService;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/voice-chat")
@CrossOrigin(origins = "*")
public class VoiceChatController {

    @Autowired
    private QiniuAsrService qiniuAsrService;

    @Autowired
    private QiniuAIService qiniuAIService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Value("${server.port:8082}")
    private String serverPort;

    /**
     * 处理语音输入，转换为文本并获取AI回复
     */
    @PostMapping(value = "/send-voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> sendVoiceMessage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("characterId") Long characterId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 使用ASR服务将语音转换为文本 (优先使用URL方式)
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
            
            // 7. 保存AI回复消息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setCharacterId(characterId);
            aiMessage.setMessage(aiResponse);
            aiMessage.setIsUserMessage(false);
            ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
            
            // 8. 构建成功响应
            response.put("success", true);
            response.put("userMessage", savedUserMessage);
            response.put("aiMessage", savedAiMessage);
            
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
            
            // 7. 保存AI回复消息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setCharacterId(characterId);
            aiMessage.setMessage(aiResponse);
            aiMessage.setIsUserMessage(false);
            ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
            
            // 8. 构建成功响应
            response.put("success", true);
            response.put("userMessage", savedUserMessage);
            response.put("aiMessage", savedAiMessage);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "处理语音消息时发生错误: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 使用URL策略进行语音转文本
     * 优先使用URL方式，如果失败则回退到data字段方式
     */
    private String transcribeWithUrlStrategy(MultipartFile file) {
        try {
            // 尝试使用URL方式（优先推荐）
            String fileUrl = saveFileAndGetUrl(file);
            return qiniuAsrService.transcribeByUrl(fileUrl);
        } catch (Exception e) {
            // 如果URL方式失败，回退到data字段方式
            try {
                return qiniuAsrService.transcribe(file);
            } catch (Exception fallbackException) {
                // 如果两种方式都失败，抛出异常
                throw new RuntimeException("ASR转录失败: URL方式错误 - " + e.getMessage() + 
                                         ", Data字段方式错误 - " + fallbackException.getMessage());
            }
        }
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