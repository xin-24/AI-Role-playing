package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.QiniuAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
            response.put("error", "角色不存在");
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

            // 按标点符号分割AI回复
            List<String> segments = splitByPunctuation(aiResponse);
            
            // 创建并保存分割后的AI回复消息
            List<ChatMessage> aiMessages = new ArrayList<>();
            for (String segment : segments) {
                if (!segment.trim().isEmpty()) {
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setCharacterId(chatMessage.getCharacterId());
                    aiMessage.setMessage(segment.trim());
                    aiMessage.setIsUserMessage(false);
                    ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                    aiMessages.add(savedAiMessage);
                }
            }
            
            response.put("success", true);
            response.put("aiMessages", aiMessages);
        } catch (Exception e) {
            // 如果AI回复生成失败，创建一个错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setCharacterId(chatMessage.getCharacterId());
            errorMessage.setMessage("抱歉，我暂时无法回复您的消息。");
            errorMessage.setIsUserMessage(false);
            ChatMessage savedErrorMessage = chatMessageRepository.save(errorMessage);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            List<ChatMessage> errorMessages = new ArrayList<>();
            errorMessages.add(savedErrorMessage);
            response.put("aiMessages", errorMessages);
        }

        return response;
    }

    // 按标点符号分割文本
    private List<String> splitByPunctuation(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        
        // 使用正则表达式按标点符号分割，保留标点符号
        Pattern pattern = Pattern.compile("([^。！？.!?]+[。！？.!?]*)");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            segments.add(matcher.group().trim());
        }
        
        // 如果没有匹配到标点符号，则将整个文本作为一个片段
        if (segments.isEmpty()) {
            segments.add(text.trim());
        }
        
        return segments;
    }

    @GetMapping("/history/{characterId}")
    public List<ChatMessage> getChatHistory(@PathVariable("characterId") Long characterId) {
        return chatMessageRepository.findByCharacterIdOrderByCreatedAtAsc(characterId);
    }
}