package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.CloudServiceProvider;
import com.ai.roleplay.service.CharacterPromptService;
import com.ai.roleplay.service.QiniuAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatMessageController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private CharacterPromptService characterPromptService;

    @Autowired
    private CloudServiceProvider cloudServiceProvider;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody ChatMessage chatMessage, HttpSession session) {
        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，抛出异常
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        chatMessage.setUserId(userId);

        Map<String, Object> response = new HashMap<>();

        // 保存用户消息
        ChatMessage savedUserMessage = chatMessageRepository.save(chatMessage);
        response.put("userMessage", savedUserMessage);

        // 获取角色信息
        Character character = null;
        if (chatMessage.getCharacterId() < 0) {
            // 处理硬编码角色
            character = getHardcodedCharacter(chatMessage.getCharacterId());
        } else {
            // 处理数据库中的角色
            character = characterRepository.findById(chatMessage.getCharacterId()).orElse(null);
        }

        if (character == null) {
            response.put("success", false);
            response.put("error", "角色不存在");
            return response;
        }

        // 获取对话历史（仅当前用户的对话历史）
        List<ChatMessage> chatHistory = chatMessageRepository
                .findByUserIdAndCharacterIdOrderByCreatedAtAsc(userId, chatMessage.getCharacterId());

        // 准备对话历史数据
        List<Map<String, Object>> historyData = chatHistory.stream().map(msg -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("isUserMessage", msg.getIsUserMessage());
            messageMap.put("message", msg.getMessage());
            return messageMap;
        }).collect(Collectors.toList());

        try {
            // 使用CloudServiceProvider获取正确的AI服务
            CloudServiceProvider.AIService aiService = cloudServiceProvider.getAIService();

            // 检查是否为硬编码角色
            String characterName = character.getName();
            String aiResponse;
            if (characterPromptService.hasCharacterPrompt(characterName)) {
                // 对于硬编码角色，使用CharacterPromptService中的提示词
                // 生成AI回复时，我们只需要传递角色名称，其他参数可以为空
                aiResponse = aiService.generateAIResponse(
                        character.getName(),
                        "", // 描述为空
                        "", // 性格特征为空
                        "", // 背景故事为空
                        historyData);
            } else {
                // 对于数据库中的角色，使用原有的方式
                aiResponse = aiService.generateAIResponse(
                        character.getName(),
                        character.getDescription(),
                        character.getPersonalityTraits(),
                        character.getBackgroundStory(),
                        historyData);
            }

            // 默认不分段回复，直接返回完整文本
            // 保留分段处理逻辑以备将来使用
            List<ChatMessage> aiMessages = new ArrayList<>();
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setUserId(userId); // 设置用户ID
            aiMessage.setCharacterId(chatMessage.getCharacterId());
            aiMessage.setMessage(aiResponse.trim());
            aiMessage.setIsUserMessage(false);
            ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
            aiMessages.add(savedAiMessage);

            response.put("success", true);
            response.put("aiMessages", aiMessages);
        } catch (Exception e) {
            // 如果AI回复生成失败，创建一个错误消息
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setUserId(userId); // 设置用户ID
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

    // 获取硬编码角色
    private Character getHardcodedCharacter(Long characterId) {
        Character character = new Character();
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
                character.setName("英语老师");
                character.setDescription("经验丰富的英语教育工作者");
                character.setPersonalityTraits("耐心、热情、严谨、富有创造力");
                character.setBackgroundStory("拥有丰富的英语理论和实践经验，致力于英语教育");
                character.setVoiceType("qiniu_zh_female_zxjxnjs");
                character.setIsDeletable(false);
                break;
            default:
                return null;
        }

        return character;
    }

    // 按标点符号分割文本，但忽略引号内的标点符号
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
                    (i == 0 || text.charAt(i - 1) != '\\')) { // 忽略转义的引号
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
            if ((c == '。' || c == '！' || c == '？' || c == '!' || c == '?') && !inQuotes) {
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

    @GetMapping("/history/{characterId}")
    public List<ChatMessage> getChatHistory(@PathVariable("characterId") Long characterId, HttpSession session) {
        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，返回空列表
        if (userId == null) {
            return new ArrayList<>();
        }

        // 只返回当前用户与指定角色的对话历史
        return chatMessageRepository.findByUserIdAndCharacterIdOrderByCreatedAtAsc(userId, characterId);
    }

    // 获取当前用户ID的接口
    @GetMapping("/user-id")
    public Map<String, String> getCurrentUserId(HttpSession session) {
        String userId = (String) session.getAttribute("user_id");
        String username = (String) session.getAttribute("username");

        Map<String, String> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", username);
        return response;
    }
}