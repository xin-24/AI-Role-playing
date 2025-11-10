package com.ai.roleplay.controller;

import com.ai.roleplay.model.ChatRequest;
import com.ai.roleplay.model.ChatResponse;
import com.ai.roleplay.service.EmotionService;
import com.ai.roleplay.service.MemoryService;
import com.ai.roleplay.utils.SensitiveFilter;
import com.ai.roleplay.service.CloudServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.CharacterPromptService;
import com.ai.roleplay.model.ChatMessage;
import com.ai.roleplay.repository.ChatMessageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private EmotionService emotionService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private CloudServiceProvider cloudServiceProvider;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private CharacterPromptService characterPromptService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> message(@RequestBody ChatRequest request) {

        String userId = request.getUserId();
        String userText = request.getText();
        Long characterId = request.getCharacterId();

        // 1. 检测敏感词
        boolean flagged = sensitiveFilter.containsSensitive(userText);
        if (flagged) {
            ChatResponse resp = new ChatResponse();
            resp.setText("我发现你的表述可能有些危险，请立即联系身边的人或求助热线。若需要，我可以帮你查找附近的求助资源。");
            resp.setEmotion("anxious");
            resp.setFlagged(true);
            // 可记录报警日志
            return ResponseEntity.ok(resp);
        }

        // 2. 情绪识别（可选择 LLM 或本地）
        String emotion = emotionService.detectEmotion(userText);

        // 3. 记忆更新（简单示例：提取兴趣关键词）
        String extractedInterest = extractInterestKeywords(userText);
        if (extractedInterest != null) {
            memoryService.saveOrUpdate(userId, "favorite_topic", extractedInterest);
        }
        memoryService.saveOrUpdate(userId, "last_mood", emotion);

        // 4. 生成 AI 回复（融合 memory/情绪）
        Map<String, String> userMemory = memoryService.readAll(userId);

        // 获取角色信息
        Character character = null;
        if (characterId < 0) {
            // 处理硬编码角色
            character = getHardcodedCharacter(characterId);
            logger.info("获取硬编码角色，ID: {}, 角色: {}", characterId, character != null ? character.getName() : "null");
        } else {
            // 处理数据库中的角色
            character = characterRepository.findById(characterId).orElse(null);
            logger.info("获取数据库角色，ID: {}, 角色: {}", characterId, character != null ? character.getName() : "null");
        }

        if (character == null) {
            logger.warn("角色不存在，ID: {}", characterId);
            ChatResponse errorResp = new ChatResponse();
            errorResp.setText("角色不存在");
            errorResp.setEmotion("neutral");
            errorResp.setFlagged(false);
            return ResponseEntity.ok(errorResp);
        }

        // 获取对话历史
        List<ChatMessage> chatHistory = chatMessageRepository
                .findByCharacterIdOrderByCreatedAtAsc(characterId);

        // 准备对话历史数据
        List<Map<String, Object>> historyData = chatHistory.stream().map(msg -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("isUserMessage", msg.getIsUserMessage());
            messageMap.put("message", msg.getMessage());
            return messageMap;
        }).collect(Collectors.toList());

        String aiText = "";
        try {
            // 使用CloudServiceProvider获取正确的AI服务
            CloudServiceProvider.AIService aiService = cloudServiceProvider.getAIService();

            // 记录角色信息用于调试
            logger.info("调用AI服务，角色ID: {}, 角色名称: {}", characterId, character.getName());

            // 检查是否为硬编码角色
            String characterName = character.getName();
            if (characterPromptService.hasCharacterPrompt(characterName)) {
                logger.info("使用硬编码角色提示词: {}", characterName);
                // 对于硬编码角色，使用CharacterPromptService中的提示词
                // 直接调用AI服务，让它自己处理硬编码角色的提示词
                aiText = aiService.generateAIResponse(
                        character.getName(),
                        character.getDescription(),
                        character.getPersonalityTraits(),
                        character.getBackgroundStory(),
                        historyData);
            } else {
                logger.info("使用数据库角色信息");
                // 对于数据库中的角色，使用原有的方式
                aiText = aiService.generateAIResponse(
                        character.getName(),
                        character.getDescription(),
                        character.getPersonalityTraits(),
                        character.getBackgroundStory(),
                        historyData);
            }
        } catch (Exception e) {
            aiText = "抱歉，我暂时无法回复您的消息。";
        }

        // 5. suggest next topics / actions
        String suggestion = generateSuggestionByEmotion(emotion);

        // 6. update companionship score
        int companionshipScore = updateCompanionshipScore(userId);

        // 7. 构建响应
        ChatResponse resp = new ChatResponse();
        resp.setText(sensitiveFilter.filterOut(aiText));
        resp.setEmotion(emotion);
        resp.setSuggestion(suggestion);
        resp.setCompanionshipScore(companionshipScore);
        resp.setFlagged(false);

        return ResponseEntity.ok(resp);
    }

    // --- helper methods: extractInterestKeywords, buildPrompt,
    // generateSuggestionByEmotion, updateCompanionshipScore

    private String extractInterestKeywords(String text) {
        // 简单的关键词提取示例
        if (text == null || text.isEmpty())
            return null;

        // 这里可以实现更复杂的关键词提取逻辑
        // 比如使用NLP库进行实体识别等
        if (text.contains("猫") || text.contains("狗") || text.contains("宠物")) {
            return "宠物";
        } else if (text.contains("音乐") || text.contains("歌曲") || text.contains("唱歌")) {
            return "音乐";
        } else if (text.contains("电影") || text.contains("电视剧") || text.contains("视频")) {
            return "影视";
        }

        return null;
    }

    private String generateSuggestionByEmotion(String emotion) {
        switch (emotion) {
            case "难受":
                return "要不要聊聊让你感到难过的事情？或者我们可以一起听首歌放松一下。";
            case "疲惫":
                return "你看起来很累呢，要不要休息一下？我可以给你讲个轻松的小故事。";
            case "焦虑":
                return "感觉你有些焦虑呢，要不要试试深呼吸？或者我们可以聊些让你开心的话题。";
            case "开心":
                return "很高兴看到你开心！要不要分享一下是什么让你这么高兴？";
            case "愤怒":
                return "看起来你有些生气呢，要不要先冷静一下？我可以陪你聊聊。";
            default:
                return "我们聊聊其他话题吧，你最近有什么有趣的经历吗？";
        }
    }

    private int updateCompanionshipScore(String userId) {
        // 从内存中读取当前的陪伴分数
        String scoreStr = memoryService.read(userId, "companionship_score");
        int currentScore = 0;
        if (scoreStr != null) {
            try {
                currentScore = Integer.parseInt(scoreStr);
            } catch (NumberFormatException e) {
                // 如果解析失败，使用默认值0
            }
        }

        // 简单的分数更新逻辑：每次交互增加1分，最多100分
        int newScore = Math.min(currentScore + 1, 100);

        // 保存更新后的分数
        memoryService.saveOrUpdate(userId, "companionship_score", String.valueOf(newScore));

        return newScore;
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
                character.setName("林暖暖");
                character.setDescription("经验丰富的心里陪伴师");
                character.setPersonalityTraits("温柔细腻，善解人意，积极阳光");
                character.setBackgroundStory("创立的" + "暖暖倾听法" + "帮助数万人缓解情绪压力，获得良好口碑");
                character.setVoiceType("qiniu_zh_female_zxjxnjs");
                character.setIsDeletable(false);
                break;
            default:
                return null;
        }

        return character;
    }
}