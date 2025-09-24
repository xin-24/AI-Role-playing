package com.ai.roleplay.service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ai.roleplay.config.QiniuASRConfig;

@Service
public class QiniuAsrService {

    private static final Logger logger = LoggerFactory.getLogger(QiniuAsrService.class);

    @Autowired
    private QiniuASRConfig qiniuASRConfig;

    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        String format = detectAudioFormat(audioFile);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = qiniuASRConfig.getBaseUrl() + "/voice/asr";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + qiniuASRConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            byte[] bytes = audioFile.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);

            JSONObject body = new JSONObject();
            body.put("model", qiniuASRConfig.getModel());

            JSONObject audio = new JSONObject();
            audio.put("format", format);
            // 优先尝试base64直传；如服务端只支持URL，可改为提供可访问的URL
            audio.put("data", base64);
            body.put("audio", audio);

            StringEntity entity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            return httpClient.execute(httpPost, response -> {
                int status = response.getCode();
                String respText = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                logger.info("ASR响应状态码: {}", status);
                logger.debug("ASR响应体: {}", respText);
                if (status == 200) {
                    try {
                        JSONObject json = new JSONObject(respText);
                        if (json.has("text")) {
                            return json.getString("text").trim();
                        }
                        // 兼容choices风格
                        if (json.has("result")) {
                            return json.getString("result").trim();
                        }
                        return respText;
                    } catch (Exception parseEx) {
                        logger.error("解析ASR响应失败: {}", parseEx.getMessage(), parseEx);
                        return respText;
                    }
                }

                String message;
                switch (status) {
                    case 400:
                        message = "请求参数错误";
                        break;
                    case 401:
                        message = "鉴权失败，请检查ASR API Key";
                        break;
                    case 403:
                        message = "无权访问ASR服务";
                        break;
                    case 429:
                        message = "调用频率超限";
                        break;
                    case 500:
                        message = "ASR服务内部错误";
                        break;
                    default:
                        message = "ASR调用失败，状态码: " + status;
                }
                throw new RuntimeException(message + "，响应: " + respText);
            });
        } catch (IOException e) {
            logger.error("调用ASR服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用ASR服务失败: " + e.getMessage(), e);
        }
    }

    private String detectAudioFormat(MultipartFile audioFile) {
        String contentType = audioFile.getContentType();
        if (contentType == null)
            return "wav";
        Map<String, String> map = new HashMap<>();
        map.put("audio/wav", "wav");
        map.put("audio/x-wav", "wav");
        map.put("audio/mpeg", "mp3");
        map.put("audio/mp3", "mp3");
        map.put("audio/ogg", "ogg");
        map.put("audio/webm", "webm");
        map.put("audio/m4a", "m4a");
        map.put("audio/aac", "aac");
        map.put("audio/flac", "flac");
        return map.getOrDefault(contentType.toLowerCase(), "wav");
    }
}
