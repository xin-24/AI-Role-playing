package com.ai.roleplay.service;

import com.ai.roleplay.config.AliyunASRConfig;
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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AliyunAsrService {

    private static final Logger logger = LoggerFactory.getLogger(AliyunAsrService.class);

    @Autowired
    private AliyunASRConfig aliyunASRConfig;

    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        try {
            // 生成本地文件路径
            String localFilePath = generateLocalFilePath(audioFile);

            // 保存文件到本地
            audioFile.transferTo(new File(localFilePath));

            logger.info("音频文件已保存到本地: {}", localFilePath);

            // 使用URL方式进行ASR转录
            return transcribeByLocalFilePath(localFilePath);
        } catch (Exception e) {
            logger.error("ASR转录失败: {}", e.getMessage(), e);
            throw new RuntimeException("ASR转录失败: " + e.getMessage(), e);
        }
    }

    // 生成本地文件路径
    private String generateLocalFilePath(MultipartFile file) {
        // 创建本地存储目录（如果不存在）
        String storageDir = "audio_files";
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成基于时间戳的文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = ".wav"; // 默认扩展名
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 使用时间戳生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "audio_" + timestamp + fileExtension;

        // 构建文件路径
        return storageDir + File.separator + filename;
    }

    // 通过本地文件路径进行ASR转录
    public String transcribeByLocalFilePath(String localFilePath) {
        try {
            // 验证文件路径是否为空
            if (localFilePath == null || localFilePath.trim().isEmpty()) {
                throw new IllegalArgumentException("本地文件路径不能为空");
            }

            // 检查文件是否存在
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("文件不存在: " + localFilePath);
            }

            // 构造文件URL（使用file://协议）
            String fileUrl = "file://" + file.getAbsolutePath();

            // 使用URL方式进行ASR转录
            return transcribeByUrl(fileUrl);
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

            // 使用阿里云专门的ASR端点
            String url = aliyunASRConfig.getBaseUrl() + "/stream/v1/asr";

            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aliyunASRConfig.getApiKey());

            // 构建符合阿里云ASR要求的请求体
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
     * 从阿里云ASR响应中提取文本
     */
    private String extractTextFromResponse(String response) {
        try {
            logger.info("解析ASR响应: {}", response);
            JSONObject json = new JSONObject(response);

            // 根据阿里云ASR API文档，检查是否有错误
            if (json.has("status") && json.getInt("status") != 20000000) {
                String errorMsg = json.optString("message", "未知错误");
                logger.error("ASR服务返回错误: {}", errorMsg);
                throw new RuntimeException("ASR服务错误: " + errorMsg);
            }

            // 提取识别结果
            if (json.has("result")) {
                String text = json.getString("result").trim();
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
     * 构建符合阿里云ASR要求的请求体（URL方式）
     */
    private JSONObject buildAsrRequestBodyByUrl(String audioUrl) {
        JSONObject body = new JSONObject();

        // 添加必要参数
        body.put("appkey", aliyunASRConfig.getModel());
        body.put("audio_url", audioUrl);

        // 添加其他配置参数
        body.put("enable_punctuation_prediction", true);
        body.put("enable_inverse_text_normalization", true);
        body.put("enable_voice_detection", false);

        // 添加调试日志，打印完整的请求体
        logger.info("ASR URL请求体: {}", body.toString());

        return body;
    }
}