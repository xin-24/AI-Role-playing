package com.ai.roleplay.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.hc.client5.http.classic.methods.HttpGet;
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

import com.ai.roleplay.config.AliyunTTSConfig;

@Service
public class AliyunTtsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunTtsService.class);
    
    // 阿里云TTS支持的音色列表
    private static final Set<String> SUPPORTED_VOICES = new HashSet<>();
    static {
        SUPPORTED_VOICES.add("Cherry");
        SUPPORTED_VOICES.add("Serena");
        SUPPORTED_VOICES.add("Ethan");
        SUPPORTED_VOICES.add("Chelsie");
    }

    @Autowired
    private AliyunTTSConfig ttsConfig;

    public byte[] synthesize(String text, String voice, String format) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("TTS文本不能为空");
        }
        
        // 转换音色为阿里云支持的音色
        String useVoice = convertVoiceToSupported(voice != null && !voice.isEmpty() ? voice : ttsConfig.getDefaultVoice());
        String useFormat = format != null && !format.isEmpty() ? format : ttsConfig.getDefaultFormat();
        
        // 检查API密钥是否已设置
        if (ttsConfig.getApiKey() == null || ttsConfig.getApiKey().isEmpty() || 
            ttsConfig.getApiKey().contains("YOUR_REAL_DASHSCOPE_API_KEY")) {
            logger.error("阿里云TTS API密钥未设置或无效");
            throw new RuntimeException("TTS服务不可用: API密钥未设置或无效");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 使用阿里云TTS RESTful API
            String url = ttsConfig.getBaseUrl() + "/services/aigc/multimodal-generation/generation";
            
            HttpPost httpPost = new HttpPost(url);
            // 使用Authorization头
            httpPost.setHeader("Authorization", "Bearer " + ttsConfig.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", ttsConfig.getModel());
            
            JSONObject input = new JSONObject();
            input.put("text", text);
            input.put("voice", useVoice);
            input.put("language_type", "Chinese");
            requestBody.put("input", input);

            httpPost.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));

            // 发送请求
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                logger.info("TTS响应状态码: {}", statusCode);
                
                if (response.getEntity() != null) {
                    byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                    if (statusCode == 200) {
                        // 解析响应
                        String responseStr = new String(responseBytes);
                        logger.debug("TTS完整响应: {}", responseStr);
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        
                        if (jsonResponse.has("output") && 
                            jsonResponse.getJSONObject("output").has("audio")) {
                            
                            JSONObject audioObj = jsonResponse.getJSONObject("output").getJSONObject("audio");
                            
                            // 检查是否有data字段且不为空
                            if (audioObj.has("data")) {
                                String base64Data = audioObj.getString("data");
                                if (base64Data != null && !base64Data.isEmpty()) {
                                    byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                                    logger.info("TTS调用成功，返回音频数据，长度: {} 字节", audioBytes.length);
                                    return audioBytes;
                                }
                            }
                            
                            // 检查是否有url字段
                            if (audioObj.has("url")) {
                                String audioUrl = audioObj.getString("url");
                                logger.info("TTS返回音频URL: {}", audioUrl);
                                // 从URL下载音频文件
                                return downloadAudioFromUrl(httpClient, audioUrl);
                            }
                        }
                        logger.error("TTS响应格式不正确: {}", responseStr);
                        throw new RuntimeException("TTS调用返回格式不正确的响应");
                    } else {
                        String errorResponse = new String(responseBytes);
                        logger.error("TTS调用失败，状态码: {}，响应: {}", statusCode, errorResponse);
                        throw new RuntimeException("TTS调用失败，状态码: " + statusCode + " 响应: " + errorResponse);
                    }
                }
                throw new RuntimeException("TTS调用失败，状态码: " + statusCode);
            });
        } catch (Exception e) {
            logger.error("调用阿里云TTS服务失败: {}", e.getMessage(), e);
            // 禁止使用模拟TTS服务作为后备方案，直接抛出异常
            throw new RuntimeException("TTS服务不可用: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将音色转换为阿里云支持的音色
     * @param voice 原始音色
     * @return 阿里云支持的音色
     */
    private String convertVoiceToSupported(String voice) {
        if (voice == null || voice.isEmpty()) {
            return ttsConfig.getDefaultVoice();
        }
        
        // 如果已经是支持的音色，直接返回
        if (SUPPORTED_VOICES.contains(voice)) {
            return voice;
        }
        
        // 音色映射转换 - 更精确的映射
        String lowerVoice = voice.toLowerCase();
        switch (lowerVoice) {
            // 女性音色映射到Serena
            case "ruoxi":
            case "siqi":
            case "sijia":
            case "xiaoyun":
            case "xiaomeng":
            case "qiniu_zh_female_wwxkjx":
            case "qiniu_zh_female_tmjxxy":
            case "qiniu_zh_female_xyqxxj":
            case "qiniu_zh_female_glktss":
            case "qiniu_zh_female_ljfdxx":
            case "qiniu_zh_female_kljxdd":
            case "qiniu_zh_female_zxjxnjs":
                return "Serena";
            // 男性音色映射到Ethan
            case "xiaogang":
            case "harry":
            case "abigail":
            case "andrew":
            case "qiniu_zh_male_ljfdxz":
            case "qiniu_zh_male_whxkxg":
            case "qiniu_zh_male_wncwxz":
            case "qiniu_zh_male_ybxknjs":
            case "qiniu_zh_male_tyygjs":
                return "Ethan";
            // 其他音色映射到Cherry
            case "xiaowei":
            case "luna":
            case "lydia":
            case "whitney":
                return "Cherry";
            // 特殊映射到Chelsie
            default:
                // 如果无法映射，使用默认音色
                logger.warn("不支持的音色: {}，使用默认音色: {}", voice, ttsConfig.getDefaultVoice());
                return ttsConfig.getDefaultVoice();
        }
    }
    
    private byte[] downloadAudioFromUrl(CloseableHttpClient httpClient, String audioUrl) {
        try {
            HttpGet httpGet = new HttpGet(audioUrl);
            return httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                if (statusCode == 200 && response.getEntity() != null) {
                    byte[] audioBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("成功下载音频文件，长度: {} 字节", audioBytes.length);
                    return audioBytes;
                } else {
                    throw new RuntimeException("下载音频文件失败，状态码: " + statusCode);
                }
            });
        } catch (Exception e) {
            logger.error("下载音频文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("下载音频文件失败: " + e.getMessage(), e);
        }
    }
}