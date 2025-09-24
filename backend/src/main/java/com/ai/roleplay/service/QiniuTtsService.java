package com.ai.roleplay.service;

import java.io.IOException;
import java.util.Base64;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.ai.roleplay.config.QiniuTTSConfig;

@Service
public class QiniuTtsService {

    private static final Logger logger = LoggerFactory.getLogger(QiniuTtsService.class);

    @Autowired
    private QiniuTTSConfig ttsConfig;

    public byte[] synthesize(String text, String voice, String format) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("TTS文本不能为空");
        }
        String useVoice = voice != null && !voice.isEmpty() ? voice : ttsConfig.getDefaultVoice();
        String useFormat = format != null && !format.isEmpty() ? format : ttsConfig.getDefaultFormat();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = ttsConfig.getBaseUrl() + "/voice/tts";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + ttsConfig.getApiKey());
            httpPost.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // 根据用户提供的示例构建请求体
            JSONObject body = new JSONObject();

            JSONObject audio = new JSONObject();
            audio.put("voice_type", useVoice);
            audio.put("encoding", useFormat);
            audio.put("speed_ratio", 1.0);
            body.put("audio", audio);

            JSONObject request = new JSONObject();
            request.put("text", text);
            body.put("request", request);

            httpPost.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

            return httpClient.execute(httpPost, response -> {
                int status = response.getCode();
                String respText = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                logger.info("TTS响应状态码: {}", status);
                logger.debug("TTS响应体: {}", respText);
                if (status == 200) {
                    try {
                        JSONObject json = new JSONObject(respText);
                        // 假设返回为 base64 音频
                        if (json.has("audio") && json.getJSONObject("audio").has("data")) {
                            String b64 = json.getJSONObject("audio").getString("data");
                            return Base64.getDecoder().decode(b64);
                        }
                        // 兼容直接字段
                        if (json.has("data")) {
                            return Base64.getDecoder().decode(json.getString("data"));
                        }
                    } catch (Exception ex) {
                        logger.error("解析TTS响应失败: {}", ex.getMessage(), ex);
                    }
                    return respText.getBytes();
                }
                throw new RuntimeException("TTS调用失败，状态码: " + status + " 响应: " + respText);
            });
        } catch (IOException e) {
            logger.error("调用TTS服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用TTS服务失败: " + e.getMessage(), e);
        }
    }
}