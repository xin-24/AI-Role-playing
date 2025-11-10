package com.ai.roleplay.controller;

import com.ai.roleplay.service.AliyunAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class LlmTestController {

    @Autowired
    private AliyunAIService aliyunAIService;

    /**
     * 测试阿里云LLM是否调用成功
     */
    @GetMapping("/llm")
    public String testLlm() {
        try {
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
            String response = aliyunAIService.generateAIResponse(
                    characterName,
                    characterDescription,
                    personalityTraits,
                    backgroundStory,
                    chatHistory);

            return "阿里云LLM调用成功！\n回复: " + response;
        } catch (Exception e) {
            return "阿里云LLM调用失败: " + e.getMessage();
        }
    }
}