package com.ai.roleplay.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.service.QiniuAIService;
import com.ai.roleplay.service.QiniuTtsService;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatMessageController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private QiniuAIService qiniuAIService;

    @Autowired
    private QiniuTtsService qiniuTtsService;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody ChatMessage chatMessage) {
        Map<String, Object> response = new HashMap<>();

        // 保存用户消息
        ChatMessage savedUserMessage = chatMessageRepository.save(chatMessage);
        response.put("userMessage", savedUserMessage);

        // 获取角色信息
        Character character = characterRepository.findById(chatMessage.getCharacterId()).orElse(null);
        if (character == null) {
            response.put("success", false);
            response.put("error", "未找到指定角色");
            return response;
        }

        // 获取对话历史
        List<ChatMessage> chatHistory = chatMessageRepository
                .findByCharacterIdOrderByCreatedAtAsc(chatMessage.getCharacterId());

        // 准备对话历史数据
        List<Map<String, Object>> historyData = chatHistory.stream().map(msg -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("isUserMessage", msg.getIsUserMessage());
            messageMap.put("message", msg.getMessage());
            return messageMap;
        }).collect(Collectors.toList());

        try {
            // 生成AI回复
            String aiResponse = qiniuAIService.generateAIResponse(
                    character.getName(),
                    character.getDescription(),
                    character.getPersonalityTraits(),
                    character.getBackgroundStory(),
                    historyData);

            // 按照标点符号（。！？）分割AI回复
            List<String> aiResponseSegments = splitByPunctuation(aiResponse);
            
            // 保存分割后的AI回复消息
            List<ChatMessage> savedAiMessages = new ArrayList<>();
            for (String segment : aiResponseSegments) {
                if (!segment.trim().isEmpty()) {
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setCharacterId(chatMessage.getCharacterId());
                    aiMessage.setMessage(segment.trim());
                    aiMessage.setIsUserMessage(false);
                    ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                    savedAiMessages.add(savedAiMessage);
                }
            }

            response.put("aiMessages", savedAiMessages);

            // 同时生成TTS语音数据（使用完整的AI回复）
            try {
                byte[] audioBytes = qiniuTtsService.synthesize(aiResponse, character.getVoiceType(), "mp3");
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                response.put("audioData", base64Audio);
                response.put("audioFormat", "mp3");
            } catch (Exception e) {
                // 如果TTS生成失败，不中断主要流程
                response.put("audioError", "语音生成失败: " + e.getMessage());
            }

            response.put("success", true);
        } catch (Exception e) {
            // 如果AI回复生成失败，创建一个错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setCharacterId(chatMessage.getCharacterId());
            errorMessage.setMessage("抱歉，我暂时无法回复您的消息。");
            errorMessage.setIsUserMessage(false);
            ChatMessage savedErrorMessage = chatMessageRepository.save(errorMessage);

            response.put("aiMessages", List.of(savedErrorMessage));
            response.put("success", true); // 仍然视为成功，只是AI回复失败
            response.put("aiError", e.getMessage());
        }

        return response;
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

    @GetMapping("/history/{characterId}")
    public List<ChatMessage> getChatHistory(@PathVariable("characterId") Long characterId) {
        return chatMessageRepository.findByCharacterIdOrderByCreatedAtAsc(characterId);
    }
}