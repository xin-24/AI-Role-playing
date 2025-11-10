package com.ai.roleplay.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class EmotionService {

    // 简单关键词映射（可扩充）
    private static final Map<String, String> KEYWORD_MAP = new HashMap<>();
    static {
        // sad
        KEYWORD_MAP.put("难过", "sad");
        KEYWORD_MAP.put("伤心", "sad");
        KEYWORD_MAP.put("不开心", "sad");
        KEYWORD_MAP.put("孤单", "sad");
        KEYWORD_MAP.put("沮丧", "sad");
        KEYWORD_MAP.put("失落", "sad");
        KEYWORD_MAP.put("郁闷", "sad");
        KEYWORD_MAP.put("忧伤", "sad");
        // tired
        KEYWORD_MAP.put("累", "tired");
        KEYWORD_MAP.put("疲惫", "tired");
        KEYWORD_MAP.put("疲劳", "tired");
        KEYWORD_MAP.put("困倦", "tired");
        KEYWORD_MAP.put("乏力", "tired");
        // anxious
        KEYWORD_MAP.put("焦虑", "anxious");
        KEYWORD_MAP.put("担心", "anxious");
        KEYWORD_MAP.put("紧张", "anxious");
        KEYWORD_MAP.put("不安", "anxious");
        KEYWORD_MAP.put("忧虑", "anxious");
        KEYWORD_MAP.put("恐慌", "anxious");
        // happy
        KEYWORD_MAP.put("开心", "happy");
        KEYWORD_MAP.put("高兴", "happy");
        KEYWORD_MAP.put("愉快", "happy");
        KEYWORD_MAP.put("喜悦", "happy");
        KEYWORD_MAP.put("兴奋", "happy");
        KEYWORD_MAP.put("欢乐", "happy");
        KEYWORD_MAP.put("欣喜", "happy");
        // angry
        KEYWORD_MAP.put("气", "angry");
        KEYWORD_MAP.put("生气", "angry");
        KEYWORD_MAP.put("愤怒", "angry");
        KEYWORD_MAP.put("恼火", "angry");
        KEYWORD_MAP.put("暴怒", "angry");
        // more...
    }

    // If you have numeric scoring, store weights
    private static final Map<String, Integer> EMO_WEIGHTS = new HashMap<>();
    static {
        EMO_WEIGHTS.put("sad", 3);
        EMO_WEIGHTS.put("tired", 2);
        EMO_WEIGHTS.put("anxious", 3);
        EMO_WEIGHTS.put("happy", 1);
        EMO_WEIGHTS.put("angry", 4);
        EMO_WEIGHTS.put("neutral", 0);
    }

    public String detectEmotion(String text) {
        if (text == null || text.isEmpty())
            return "neutral";
        String lower = text.toLowerCase();
        Map<String, Integer> score = new HashMap<>();

        // 初始化所有情绪的分数为0
        for (String emotion : EMO_WEIGHTS.keySet()) {
            score.put(emotion, 0);
        }

        // 计算每种情绪的分数
        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            String kw = entry.getKey();
            String emo = entry.getValue();
            if (lower.contains(kw)) {
                score.put(emo, score.getOrDefault(emo, 0) + EMO_WEIGHTS.getOrDefault(emo, 1));
            }
        }

        // Basic heuristic: highest score wins
        return score.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");
    }

    /**
     * Optional: call external LLM/emotion API if you need higher precision.
     * Example pseudo-method - implement using your chosen LLM client.
     */
    public String detectEmotionWithLLM(String text) {
        // 1. Build prompt: "Classify the emotion of the following Chinese text into one
        // of: happy, sad, tired, anxious, angry, neutral..."
        // 2. Call LLM API (e.g., via HttpClient) and parse result.
        // For contest, keyword method is acceptable and reliable.
        return detectEmotion(text); // fallback
    }

    // 添加一个简单的测试方法
    public boolean isServiceAvailable() {
        return true;
    }
}