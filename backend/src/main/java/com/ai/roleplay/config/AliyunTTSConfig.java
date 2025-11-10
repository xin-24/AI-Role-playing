package com.ai.roleplay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AliyunTTSConfig {

    @Value("${tts.aliyun.api-key}")
    private String apiKey;

    @Value("${tts.aliyun.base-url}")
    private String baseUrl;

    @Value("${tts.aliyun.model:qwen3-tts-flash}")
    private String model;

    @Value("${tts.aliyun.voice:Cherry}")
    private String defaultVoice;

    @Value("${tts.aliyun.format:mp3}")
    private String defaultFormat;

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

    public String getDefaultVoice() {
        return defaultVoice;
    }

    public void setDefaultVoice(String defaultVoice) {
        this.defaultVoice = defaultVoice;
    }

    public String getDefaultFormat() {
        return defaultFormat;
    }

    public void setDefaultFormat(String defaultFormat) {
        this.defaultFormat = defaultFormat;
    }
    
    /**
     * 检查API密钥是否有效
     * @return true if API key is valid, false otherwise
     */
    public boolean isApiKeyValid() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.contains("YOUR_REAL");
    }
}