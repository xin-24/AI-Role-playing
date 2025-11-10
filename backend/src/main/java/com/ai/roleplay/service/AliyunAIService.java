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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ai.roleplay.config.AliyunAIConfig;

@Service
public class AliyunAIService {

    private static final Logger logger = LoggerFactory.getLogger(AliyunAIService.class);

    @Autowired
    private AliyunAIConfig aliyunAIConfig;

    @Autowired
    private CharacterPromptService characterPromptService;

    // 从配置文件读取最大回复长度，默认为500字符
    @Value("${ai.max.response.length:500}")
    private int maxResponseLength;

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

        // 使用硬编码的角色提示词
        String systemPrompt = characterPromptService.getCharacterPrompt(characterName);

        // 如果没有硬编码的提示词，则使用原有的动态构建方式
        if (systemPrompt.equals(characterPromptService.getDefaultPrompt())) {
            // 构建更自然的角色设定提示
            StringBuilder dynamicPrompt = new StringBuilder();
            dynamicPrompt.append("你是一个角色扮演AI，严格根据以下设定进行回复：\n");
            dynamicPrompt.append("角色名称：").append(characterName).append("\n");
            dynamicPrompt.append("角色描述：").append(characterDescription).append("\n");
            dynamicPrompt.append("性格特征：").append(personalityTraits).append("\n");
            dynamicPrompt.append("背景故事：").append(backgroundStory).append("\n");
            dynamicPrompt.append("请始终保持这个角色的身份，用符合角色性格和背景的方式进行回复，可以使用对应的颜文字或者使用emoji表情，限制200字符");
            systemPrompt = dynamicPrompt.toString();
        }

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
        logger.info("调用阿里云AI API，角色名称: {}", characterName);
        logger.debug("系统提示词: {}", systemPrompt);
        logger.debug("用户消息: {}", conversationHistory.toString());

        // 调用阿里云AI API
        String response = callAliyunAI(systemPrompt, conversationHistory.toString());

        // 限制回复长度
        if (response != null && response.length() > maxResponseLength) {
            logger.info("AI回复长度超过限制，截取前{}个字符", maxResponseLength);
            response = response.substring(0, maxResponseLength);
        }

        return response;
    }

    /**
     * 调用阿里云AI API
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param userMessage  用户消息和对话历史
     * @return AI回复内容
     */
    private String callAliyunAI(String systemPrompt, String userMessage) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 构建阿里云AI API URL
            String url = aliyunAIConfig.getBaseUrl() + "/services/aigc/text-generation/generation";
            logger.info("调用阿里云AI API，URL: {}", url);
            logger.info("API密钥前缀: {}",
                    aliyunAIConfig.getApiKey().substring(0, Math.min(10, aliyunAIConfig.getApiKey().length())) + "...");

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + aliyunAIConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("X-DashScope-SSE", "enable");

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", aliyunAIConfig.getModel());

            // 构建消息数组
            JSONObject input = new JSONObject();

            // 构建消息内容
            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(systemPrompt).append("\n\n");
            fullPrompt.append(userMessage);

            input.put("prompt", fullPrompt.toString());
            requestBody.put("input", input);

            // 添加参数
            JSONObject parameters = new JSONObject();
            parameters.put("temperature", 0.7);
            parameters.put("max_tokens", 500);
            requestBody.put("parameters", parameters);

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // 执行请求
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                logger.info("阿里云AI API响应状态码: {}", statusCode);

                String responseBody = "";
                try {
                    responseBody = EntityUtils.toString(response.getEntity());
                    logger.info("阿里云AI API响应体长度: {} 字符", responseBody.length());
                    logger.debug("阿里云AI API响应体: {}", responseBody);
                } catch (Exception e) {
                    logger.error("读取阿里云AI API响应体失败: {}", e.getMessage(), e);
                    throw new RuntimeException("读取阿里云AI API响应体失败: " + e.getMessage(), e);
                }

                if (statusCode == 200) {
                    try {
                        // 检查响应体是否为空
                        if (responseBody == null || responseBody.trim().isEmpty()) {
                            logger.error("阿里云AI API返回空响应");
                            throw new RuntimeException("阿里云AI API返回空响应");
                        }

                        // 处理流式响应，提取最后一条包含完整结果的消息
                        String finalText = extractFinalTextFromStreamResponse(responseBody);
                        return finalText;
                    } catch (Exception e) {
                        logger.error("解析阿里云AI响应失败: {}", e.getMessage(), e);
                        throw new RuntimeException("解析阿里云AI响应失败: " + e.getMessage(), e);
                    }
                } else {
                    logger.error("阿里云AI API调用失败，状态码: {}，响应体: {}", statusCode, responseBody);

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
                            errorMessage = "阿里云AI服务内部错误，请稍后再试";
                            break;
                        default:
                            errorMessage = "阿里云AI API调用失败，状态码: " + statusCode;
                    }

                    throw new RuntimeException(errorMessage + "，响应: " + responseBody);
                }
            });
        } catch (IOException e) {
            logger.error("调用阿里云AI API时发生IO异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用阿里云AI API时发生IO异常: " + e.getMessage() +
                    "。请检查网络连接或防火墙设置。", e);
        }
    }

    /**
     * 从流式响应中提取最终文本
     *
     * @param streamResponse 流式响应内容
     * @return 最终生成的文本
     */
    private String extractFinalTextFromStreamResponse(String streamResponse) {
        try {
            // 按行分割响应
            String[] lines = streamResponse.split("\n");
            String finalText = "";
            boolean isFinished = false;

            // 记录响应行数用于调试
            logger.debug("流式响应行数: {}", lines.length);

            // 遍历所有行，找到包含data的行并提取文本
            for (String line : lines) {
                logger.debug("处理响应行: {}", line);
                if (line.startsWith("data:")) {
                    String jsonData = line.substring(5).trim(); // 去掉"data:"前缀
                    try {
                        JSONObject json = new JSONObject(jsonData);
                        if (json.has("output") && json.getJSONObject("output").has("text")) {
                            finalText = json.getJSONObject("output").getString("text").trim();
                            logger.debug("提取到文本: {}", finalText);
                        }

                        // 检查是否已完成
                        if (json.has("output") && json.getJSONObject("output").has("finish_reason")) {
                            String finishReason = json.getJSONObject("output").getString("finish_reason");
                            logger.debug("完成原因: {}", finishReason);
                            if ("stop".equals(finishReason)) {
                                isFinished = true;
                                logger.debug("检测到完成标记");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("解析流式响应中的单行数据失败: {}", e.getMessage());
                        logger.debug("失败的JSON数据: {}", jsonData);
                    }
                }
            }

            // 如果找到了完成标记，返回最终文本
            if (isFinished) {
                logger.debug("返回完成的文本: {}", finalText);
                return finalText;
            }

            // 如果没有找到完成标记，但有文本内容，也返回文本
            if (!finalText.isEmpty()) {
                logger.debug("返回非空文本: {}", finalText);
                return finalText;
            }

            // 如果没有有效内容，返回原始响应
            logger.warn("未提取到有效文本，返回原始响应");
            return streamResponse;
        } catch (Exception e) {
            logger.error("从流式响应中提取最终文本失败: {}", e.getMessage(), e);
            return streamResponse;
        }
    }

    // 添加getter方法，便于测试
    public CharacterPromptService getCharacterPromptService() {
        return characterPromptService;
    }

    // 添加maxResponseLength的getter和setter方法，便于测试和配置
    public int getMaxResponseLength() {
        return maxResponseLength;
    }

    public void setMaxResponseLength(int maxResponseLength) {
        this.maxResponseLength = maxResponseLength;
    }
}