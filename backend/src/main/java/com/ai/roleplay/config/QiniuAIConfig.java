package com.ai.roleplay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QiniuAIConfig {
    @Value("${qiniu.ai.api-key}")
    private String apiKey;

    @Value("${qiniu.ai.base-url}")
    private String baseUrl;

    @Value("${qiniu.ai.model}")
    private String model;

    // Getters and Setters
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}