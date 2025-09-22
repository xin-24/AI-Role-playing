package com.ai.roleplay.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.roleplay.config.QiniuAIConfig;

@Service
public class QiniuAIService {

    private static final Logger logger = LoggerFactory.getLogger(QiniuAIService.class);

    @Autowired
    private QiniuAIConfig qiniuAIConfig;

    /**
     * 根据用户输入的角色设定和对话历史生成AI回复
     *
     * @param characterName        角色名称
     * @param characterDescription 角色描述
     * @param personalityTraits    性格特征
     * @param backgroundStory      背景故事
     * @param chatHistory          对话历史
     * @return AI回复内容
     */
    public String generateAIResponse(String characterName, String characterDescription,
            String personalityTraits, String backgroundStory,
            List<Map<String, Object>> chatHistory) {

        // 构建更自然的角色设定提示
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个角色扮演AI，严格根据以下设定进行回复：\n");
        systemPrompt.append("角色名称：").append(characterName).append("\n");
        systemPrompt.append("角色描述：").append(characterDescription).append("\n");
        systemPrompt.append("性格特征：").append(personalityTraits).append("\n");
        systemPrompt.append("背景故事：").append(backgroundStory).append("\n");
        systemPrompt.append("请始终保持这个角色的身份，用符合角色性格和背景的方式进行回复。");

        // 构建对话历史消息
        StringBuilder conversationHistory = new StringBuilder();
        for (Map<String, Object> message : chatHistory) {
            Boolean isUserMessage = (Boolean) message.get("isUserMessage");
            String content = (String) message.get("message");
            if (isUserMessage) {
                conversationHistory.append("用户: ").append(content).append("\n");
            } else {
                conversationHistory.append("AI: ").append(content).append("\n");
            }
        }

        // 记录请求信息用于调试
        logger.info("调用七牛AI API，角色名称: {}, 角色描述: {}, 性格特征: {}", characterName, characterDescription, personalityTraits);
        logger.debug("系统提示词: {}", systemPrompt.toString());
        logger.debug("用户消息: {}", conversationHistory.toString());

        // 调用七牛AI API
        return callQiniuAI(systemPrompt.toString(), conversationHistory.toString());
    }

    /**
     * 调用七牛AI API
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param userMessage  用户消息和对话历史
     * @return AI回复内容
     */
    private String callQiniuAI(String systemPrompt, String userMessage) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 修复URL构建问题，baseUrl已经包含/v1路径
            String url = qiniuAIConfig.getBaseUrl() + "/chat/completions";
            logger.info("调用七牛AI API，URL: {}", url);
            logger.info("API密钥前缀: {}",
                    qiniuAIConfig.getApiKey().substring(0, Math.min(10, qiniuAIConfig.getApiKey().length())) + "...");

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + qiniuAIConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", qiniuAIConfig.getModel());

            // 构建消息数组
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            JSONObject userMessageObj = new JSONObject();
            userMessageObj.put("role", "user");
            userMessageObj.put("content", userMessage);

            requestBody.put("messages", new JSONObject[] { systemMessage, userMessageObj });
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // 执行请求
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                logger.info("七牛AI API响应状态码: {}", statusCode);

                if (statusCode == 200) {
                    try {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        logger.debug("七牛AI API响应体: {}", responseBody);

                        JSONObject jsonResponse = new JSONObject(responseBody);
                        return jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();
                    } catch (Exception e) {
                        logger.error("解析七牛AI响应失败: {}", e.getMessage(), e);
                        throw new RuntimeException("解析七牛AI响应失败: " + e.getMessage(), e);
                    }
                } else {
                    String errorBody = "";
                    try {
                        errorBody = EntityUtils.toString(response.getEntity());
                        logger.error("七牛AI API调用失败，状态码: {}，响应体: {}", statusCode, errorBody);
                    } catch (Exception e) {
                        logger.error("获取七牛AI API错误响应失败: {}", e.getMessage(), e);
                    }

                    // 根据不同的错误状态码提供更具体的错误信息
                    String errorMessage;
                    switch (statusCode) {
                        case 401:
                            errorMessage = "API密钥无效或已过期，请检查配置";
                            break;
                        case 403:
                            errorMessage = "API访问被拒绝，请检查权限设置";
                            break;
                        case 429:
                            errorMessage = "API调用频率超限，请稍后再试";
                            break;
                        case 500:
                            errorMessage = "七牛AI服务内部错误，请稍后再试";
                            break;
                        default:
                            errorMessage = "七牛AI API调用失败，状态码: " + statusCode;
                    }

                    throw new RuntimeException(errorMessage);
                }
            });
        } catch (IOException e) {
            logger.error("调用七牛AI API时发生IO异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用七牛AI API时发生IO异常: " + e.getMessage() +
                    "。请检查网络连接或防火墙设置。", e);
        }
    }
}