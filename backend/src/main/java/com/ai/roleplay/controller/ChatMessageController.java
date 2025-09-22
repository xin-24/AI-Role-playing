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
import java.util.stream.Collectors;

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
    public ChatMessage sendMessage(@RequestBody ChatMessage chatMessage) {
        // 保存用户消息
        ChatMessage savedUserMessage = chatMessageRepository.save(chatMessage);

        // 获取角色信息
        Character character = characterRepository.findById(chatMessage.getCharacterId()).orElse(null);
        if (character == null) {
            // 如果找不到角色，只返回用户消息
            return savedUserMessage;
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

            // 创建并保存AI回复消息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setCharacterId(chatMessage.getCharacterId());
            aiMessage.setMessage(aiResponse);
            aiMessage.setIsUserMessage(false);
            chatMessageRepository.save(aiMessage);
        } catch (Exception e) {
            // 如果AI回复生成失败，创建一个错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setCharacterId(chatMessage.getCharacterId());
            errorMessage.setMessage("抱歉，我暂时无法回复您的消息。");
            errorMessage.setIsUserMessage(false);
            chatMessageRepository.save(errorMessage);
        }

        return savedUserMessage;
    }

    @GetMapping("/history/{characterId}")
    public List<ChatMessage> getChatHistory(@PathVariable("characterId") Long characterId) {
        return chatMessageRepository.findByCharacterIdOrderByCreatedAtAsc(characterId);
    }
}