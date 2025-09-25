package com.ai.roleplay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QiniuASRConfig {

    @Value("${asr.qiniu.api-key}")
    private String apiKey;

    @Value("${asr.qiniu.base-url}")
    private String baseUrl;

    @Value("${asr.qiniu.model}")
    private String model;

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



