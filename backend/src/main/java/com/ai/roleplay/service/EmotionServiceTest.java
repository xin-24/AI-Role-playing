package com.ai.roleplay.service;

public class EmotionServiceTest {
    public static void main(String[] args) {
        EmotionService emotionService = new EmotionService();

        // 测试 sad 情绪
        String result = emotionService.detectEmotion("我今天很难过");
        System.out.println("Text: 我今天很难过, Emotion: " + result);

        // 测试 happy 情绪
        result = emotionService.detectEmotion("我今天很开心");
        System.out.println("Text: 我今天很开心, Emotion: " + result);

        // 测试 tired 情绪
        result = emotionService.detectEmotion("我很累");
        System.out.println("Text: 我很累, Emotion: " + result);

        // 测试 anxious 情绪
        result = emotionService.detectEmotion("我很焦虑");
        System.out.println("Text: 我很焦虑, Emotion: " + result);

        // 测试 angry 情绪
        result = emotionService.detectEmotion("我很生气");
        System.out.println("Text: 我很生气, Emotion: " + result);

        // 测试 neutral 情绪
        result = emotionService.detectEmotion("今天天气不错");
        System.out.println("Text: 今天天气不错, Emotion: " + result);

        // 测试复合情绪
        result = emotionService.detectEmotion("我既开心又有点焦虑");
        System.out.println("Text: 我既开心又有点焦虑, Emotion: " + result);
    }
}