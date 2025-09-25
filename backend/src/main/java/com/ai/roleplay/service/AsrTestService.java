package com.ai.roleplay.service;

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

import com.ai.roleplay.config.QiniuASRConfig;

@Service
public class AsrTestService {

    private static final Logger logger = LoggerFactory.getLogger(AsrTestService.class);

    @Autowired
    private QiniuASRConfig qiniuASRConfig;

    public String testAsr() {
        try {
            // 使用七牛云专门的ASR端点 (使用配置的baseUrl)
            String url = qiniuASRConfig.getBaseUrl() + "/voice/asr";

            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(qiniuASRConfig.getApiKey());

            // 构建符合七牛云ASR要求的请求体
            JSONObject body = new JSONObject();
            body.put("model", "asr");

            JSONObject audio = new JSONObject();
            audio.put("format", "mp3");
            audio.put("url", "https://static.qiniu.com/ai-inference/example-resources/example.mp3");
            body.put("audio", audio);

            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            logger.info(
                    "发送ASR测试请求: URL={}, Headers=Authorization:Bearer [REDACTED], Content-Type:application/json, Body={}",
                    url, body.toString());

            // 发送请求
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            int status = response.getStatusCode().value();
            String respText = response.getBody();
            logger.info("ASR测试响应状态码: {}, 响应体: {}", status, respText);

            return "状态码: " + status + ", 响应: " + respText;
        } catch (Exception e) {
            logger.error("调用ASR测试服务失败: {}", e.getMessage(), e);
            return "调用ASR测试服务失败: " + e.getMessage();
        }
    }
}