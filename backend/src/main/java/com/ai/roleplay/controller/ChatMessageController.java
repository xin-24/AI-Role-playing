package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.ChatMessageRepository;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.QiniuAIService;
import com.ai.roleplay.service.CharacterPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Optional;

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
    private CharacterPromptService characterPromptService;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody ChatMessage chatMessage) {
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
            // 检查是否为硬编码角色
            String characterName = character.getName();
            if (characterPromptService.hasCharacterPrompt(characterName)) {
                // 对于硬编码角色，使用CharacterPromptService中的提示词
                // 生成AI回复时，我们只需要传递角色名称，其他参数可以为空
                String aiResponse = qiniuAIService.generateAIResponse(
                        character.getName(),
                        "", // 描述为空
                        "", // 性格特征为空
                        "", // 背景故事为空
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
            } else {
                // 对于数据库中的角色，使用原有的方式
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
            }
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
            if ((c == '。' || c == '！' || c == '？'  || c == '!' || c == '?') && !inQuotes) {
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
    public List<ChatMessage> getChatHistory(@PathVariable("characterId") Long characterId) {
        return chatMessageRepository.findByCharacterIdOrderByCreatedAtAsc(characterId);
    }
}