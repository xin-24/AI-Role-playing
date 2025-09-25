package com.ai.roleplay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QiniuTTSConfig {

    @Value("${tts.qiniu.api-key}")
    private String apiKey;

    @Value("${tts.qiniu.base-url}")
    private String baseUrl;

    @Value("${tts.qiniu.model:tts-1}")
    private String model;

    @Value("${tts.qiniu.voice:zh-CN-Yunxi}")
    private String defaultVoice;

    @Value("${tts.qiniu.format:mp3}")
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
}


