package com.ai.roleplay;

import com.ai.roleplay.service.CharacterPromptService;

public class TestPrompt {
    public static void main(String[] args) {
        CharacterPromptService service = new CharacterPromptService();

        // 测试哈利·波特的提示词
        String harryPrompt = service.getCharacterPrompt("哈利·波特");
        System.out.println("哈利·波特提示词:");
        System.out.println(harryPrompt);
        System.out.println("---");

        // 测试默认提示词
        String defaultPrompt = service.getDefaultPrompt();
        System.out.println("默认提示词:");
        System.out.println(defaultPrompt);
        System.out.println("---");

        // 检查是否能找到哈利·波特的提示词
        boolean hasHarryPrompt = service.hasCharacterPrompt("哈利·波特");
        System.out.println("是否有哈利·波特提示词: " + hasHarryPrompt);
    }
}