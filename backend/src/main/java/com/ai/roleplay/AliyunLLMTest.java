package com.ai.roleplay;

import com.ai.roleplay.service.AliyunAIService;
import com.ai.roleplay.config.AliyunAIConfig;
import com.ai.roleplay.service.CharacterPromptService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云LLM调用测试类
 * 
 * 此类用于测试阿里云LLM服务是否配置正确并能正常调用
 */
public class AliyunLLMTest {

    public static void main(String[] args) {
        try {
            // 创建阿里云AI配置
            AliyunAIConfig config = new AliyunAIConfig();
            config.setApiKey(System.getenv("DASHSCOPE_API_KEY")); // 从环境变量获取API Key
            config.setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
            config.setModel("qwen-plus"); // 使用qwen-plus模型

            // 创建角色提示服务
            CharacterPromptService characterPromptService = new CharacterPromptService();

            // 创建阿里云AI服务实例
            AliyunAIService aiService = new AliyunAIService();
            // 使用反射设置私有字段（在实际应用中，Spring会自动注入）
            java.lang.reflect.Field configField = AliyunAIService.class.getDeclaredField("aliyunAIConfig");
            configField.setAccessible(true);
            configField.set(aiService, config);

            java.lang.reflect.Field promptServiceField = AliyunAIService.class
                    .getDeclaredField("characterPromptService");
            promptServiceField.setAccessible(true);
            promptServiceField.set(aiService, characterPromptService);

            // 设置角色信息
            String characterName = "测试角色";
            String characterDescription = "一个用于测试的AI角色";
            String personalityTraits = "友好、乐于助人";
            String backgroundStory = "这是一个测试角色，用于验证阿里云LLM服务是否正常工作";

            // 创建简单的对话历史
            List<Map<String, Object>> chatHistory = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("isUserMessage", true);
            userMessage.put("message", "你好，这是一个测试消息");
            chatHistory.add(userMessage);

            // 调用阿里云AI服务生成回复
            String response = aiService.generateAIResponse(
                    characterName,
                    characterDescription,
                    personalityTraits,
                    backgroundStory,
                    chatHistory);

            System.out.println("阿里云LLM调用成功！");
            System.out.println("角色: " + characterName);
            System.out.println("回复: " + response);
        } catch (Exception e) {
            System.err.println("阿里云LLM调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}