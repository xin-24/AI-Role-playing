package com.ai.roleplay.service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
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

        // 检测音频格式
        String format = detectAudioFormat(audioFile);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = qiniuASRConfig.getBaseUrl() + "/voice/asr";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + qiniuASRConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            // 构建符合七牛云要求的请求体
            JSONObject requestBody = buildAsrRequestBody(audioFile, format);

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            logger.info("发送ASR请求: URL={}, Body={}", url, requestBody.toString());

            return httpClient.execute(httpPost, response -> {
                int status = response.getCode();
                String respText = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                logger.info("ASR响应状态码: {}, 响应体: {}", status, respText);

                if (status == 200) {
                    return extractTextFromResponse(respText);
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

    // 添加一个使用URL的转录方法
    public String transcribeByUrl(String audioUrl) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = qiniuASRConfig.getBaseUrl() + "/voice/asr";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + qiniuASRConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            // 构建符合七牛云要求的请求体
            JSONObject requestBody = buildAsrRequestBodyByUrl(audioUrl);

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            logger.info("发送ASR请求: URL={}, Body={}", url, requestBody.toString());

            return httpClient.execute(httpPost, response -> {
                int status = response.getCode();
                String respText = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                logger.info("ASR响应状态码: {}, 响应体: {}", status, respText);

                if (status == 200) {
                    return extractTextFromResponse(respText);
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

    /**
     * 从七牛云ASR响应中提取文本
     */
    private String extractTextFromResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            // 根据七牛云ASR API文档，响应结构包含data字段
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                if (data.has("result")) {
                    JSONObject result = data.getJSONObject("result");
                    if (result.has("text")) {
                        return result.getString("text").trim();
                    }
                }
            }
            // 如果没有找到text字段，返回原始响应
            return response;
        } catch (Exception e) {
            logger.error("解析ASR响应失败: {}", e.getMessage(), e);
            return response;
        }
    }

    /**
     * 构建符合七牛云ASR要求的请求体（文件上传方式）
     */
    private JSONObject buildAsrRequestBody(MultipartFile audioFile, String format) throws IOException {
        JSONObject body = new JSONObject();

        // 添加模型名称
        body.put("model", qiniuASRConfig.getModel());

        // 添加音频信息
        JSONObject audio = new JSONObject();
        audio.put("format", format); // 使用实际的音频格式
        // 对于文件上传方式，使用data字段
        String base64Data = Base64.getEncoder().encodeToString(audioFile.getBytes());
        audio.put("data", base64Data);
        body.put("audio", audio);

        return body;
    }

    /**
     * 构建符合七牛云ASR要求的请求体（URL方式）
     */
    private JSONObject buildAsrRequestBodyByUrl(String audioUrl) {
        JSONObject body = new JSONObject();

        // 添加模型名称
        body.put("model", qiniuASRConfig.getModel());

        // 添加音频信息
        JSONObject audio = new JSONObject();
        // 根据URL后缀判断音频格式
        String format = detectAudioFormatFromUrl(audioUrl);
        audio.put("format", format); // 使用实际的音频格式
        // 对于URL方式，必须使用url字段
        audio.put("url", audioUrl);
        body.put("audio", audio);

        return body;
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

    // 根据URL后缀判断音频格式
    private String detectAudioFormatFromUrl(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return "wav";
        }

        String lowerUrl = audioUrl.toLowerCase();
        if (lowerUrl.endsWith(".mp3")) {
            return "mp3";
        } else if (lowerUrl.endsWith(".wav")) {
            return "wav";
        } else if (lowerUrl.endsWith(".m4a")) {
            return "m4a";
        } else if (lowerUrl.endsWith(".flac")) {
            return "flac";
        } else if (lowerUrl.endsWith(".aac")) {
            return "aac";
        } else if (lowerUrl.endsWith(".ogg")) {
            return "ogg";
        } else if (lowerUrl.endsWith(".webm")) {
            return "webm";
        }

        // 默认返回mp3
        return "mp3";
    }
}