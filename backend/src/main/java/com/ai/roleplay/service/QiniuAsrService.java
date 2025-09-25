package com.ai.roleplay.service;

import com.ai.roleplay.config.QiniuASRConfig;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QiniuAsrService {

    private static final Logger logger = LoggerFactory.getLogger(QiniuAsrService.class);

    @Autowired
    private QiniuASRConfig qiniuASRConfig;

    @Autowired
    private QiniuOSSService qiniuOSSService;

    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        try {
            // 将音频文件上传到七牛云对象存储
            String audioUrl = qiniuOSSService.uploadFile(audioFile);
            logger.info("音频文件已上传到: {}", audioUrl);

            // 使用URL方式进行ASR转录
            return transcribeByUrl(audioUrl);
        } catch (Exception e) {
            logger.error("ASR转录失败: {}", e.getMessage(), e);
            throw new RuntimeException("ASR转录失败: " + e.getMessage(), e);
        }
    }

    // 添加一个使用URL的转录方法
    public String transcribeByUrl(String audioUrl) {
        try {
            // 验证音频URL是否为空
            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("音频URL不能为空");
            }
            
            // 使用七牛云专门的ASR端点 (使用配置的baseUrl)
            String url = qiniuASRConfig.getBaseUrl() + "/voice/asr";

            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(qiniuASRConfig.getApiKey());

            // 构建符合七牛云ASR要求的请求体
            JSONObject requestBody = buildAsrRequestBodyByUrl(audioUrl);

            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            logger.info(
                    "发送ASR请求: URL={}, Headers=Authorization:Bearer [REDACTED], Content-Type:application/json, Body={}",
                    url, requestBody.toString());

            // 记录音频URL以供调试
            logger.info("请求的音频URL: {}", audioUrl);

            // 发送请求
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            int status = response.getStatusCode().value();
            String respText = response.getBody();
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
        } catch (Exception e) {
            logger.error("调用ASR服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用ASR服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从七牛云ASR响应中提取文本
     */
    private String extractTextFromResponse(String response) {
        try {
            logger.info("解析ASR响应: {}", response);
            JSONObject json = new JSONObject(response);

            // 根据七牛云ASR API文档，检查是否有错误
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error.optString("message", "未知错误");
                logger.error("ASR服务返回错误: {}", errorMsg);
                throw new RuntimeException("ASR服务错误: " + errorMsg);
            }

            // 尝试多种可能的响应格式
            // 格式1: { "result": { "text": "识别的文本" } }
            if (json.has("result")) {
                JSONObject result = json.getJSONObject("result");
                if (result.has("text")) {
                    String text = result.getString("text").trim();
                    logger.info("成功提取文本: {}", text);
                    return text;
                }
            }

            // 格式2: { "data": { "result": { "text": "识别的文本" } } }
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                if (data.has("result")) {
                    JSONObject result = data.getJSONObject("result");
                    if (result.has("text")) {
                        String text = result.getString("text").trim();
                        logger.info("成功提取文本: {}", text);
                        return text;
                    }
                }
            }

            // 格式3: 直接返回文本（某些情况下）
            if (json.has("text")) {
                String text = json.getString("text").trim();
                logger.info("成功提取文本: {}", text);
                return text;
            }

            // 如果没有找到text字段，返回原始响应
            logger.warn("无法从响应中提取文本，返回原始响应: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("解析ASR响应失败: {}", e.getMessage(), e);
            return response;
        }
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
        audio.put("format", format);
        audio.put("url", audioUrl);
        body.put("audio", audio);

        // 添加调试日志，打印完整的请求体
        logger.info("ASR URL请求体: {}", body.toString());

        return body;
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

        // 默认返回wav
        return "wav";
    }
}