package com.ai.roleplay.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class CloudServiceProvider {

    @Value("${cloud.service.provider:qiniu}")
    private String serviceProvider;

    @Autowired
    private QiniuAsrServiceAdapter qiniuAsrService;

    @Autowired
    private QiniuTtsServiceAdapter qiniuTtsService;

    @Autowired
    private QiniuAIServiceAdapter qiniuAIService;

    @Autowired
    private QiniuOSSServiceAdapter qiniuOSSService;

    @Autowired
    private AliyunAsrServiceAdapter aliyunAsrService;

    @Autowired
    private AliyunTtsServiceAdapter aliyunTtsService;

    @Autowired
    private AliyunAIServiceAdapter aliyunAIService;

    @Autowired
    private AliyunOSSServiceAdapter aliyunOSSService;

    public AsrService getAsrService() {
        if ("aliyun".equalsIgnoreCase(serviceProvider)) {
            return aliyunAsrService;
        }
        return qiniuAsrService;
    }

    public TtsService getTtsService() {
        if ("aliyun".equalsIgnoreCase(serviceProvider)) {
            return aliyunTtsService;
        }
        return qiniuTtsService;
    }

    public AIService getAIService() {
        if ("aliyun".equalsIgnoreCase(serviceProvider)) {
            return aliyunAIService;
        }
        return qiniuAIService;
    }

    public OssService getOssService() {
        if ("aliyun".equalsIgnoreCase(serviceProvider)) {
            return aliyunOSSService;
        }
        return qiniuOSSService;
    }

    // 接口定义
    public interface AsrService {
        String transcribeByUrl(String audioUrl);
    }

    public interface TtsService {
        byte[] synthesize(String text, String voice, String format);
    }

    public interface AIService {
        String generateAIResponse(String characterName, String characterDescription,
                String personalityTraits, String backgroundStory,
                List<Map<String, Object>> chatHistory);
    }

    public interface OssService {
        String uploadFile(MultipartFile file);
    }

    // 适配器类
    @Service
    public static class QiniuAsrServiceAdapter implements AsrService {
        @Autowired
        private QiniuAsrService qiniuAsrService;

        @Override
        public String transcribeByUrl(String audioUrl) {
            return qiniuAsrService.transcribeByUrl(audioUrl);
        }
    }

    @Service
    public static class QiniuTtsServiceAdapter implements TtsService {
        @Autowired
        private QiniuTtsService qiniuTtsService;

        @Override
        public byte[] synthesize(String text, String voice, String format) {
            return qiniuTtsService.synthesize(text, voice, format);
        }
    }

    @Service
    public static class QiniuAIServiceAdapter implements AIService {
        @Autowired
        private QiniuAIService qiniuAIService;

        @Override
        public String generateAIResponse(String characterName, String characterDescription,
                String personalityTraits, String backgroundStory,
                List<Map<String, Object>> chatHistory) {
            return qiniuAIService.generateAIResponse(characterName, characterDescription,
                    personalityTraits, backgroundStory, chatHistory);
        }
    }

    @Service
    public static class QiniuOSSServiceAdapter implements OssService {
        @Autowired
        private QiniuOSSService qiniuOSSService;

        @Override
        public String uploadFile(MultipartFile file) {
            return qiniuOSSService.uploadFile(file);
        }
    }

    @Service
    public static class AliyunAsrServiceAdapter implements AsrService {
        @Autowired
        private AliyunAsrService aliyunAsrService;

        @Override
        public String transcribeByUrl(String audioUrl) {
            return aliyunAsrService.transcribeByUrl(audioUrl);
        }
    }

    @Service
    public static class AliyunTtsServiceAdapter implements TtsService {
        @Autowired
        private AliyunTtsService aliyunTtsService;

        @Override
        public byte[] synthesize(String text, String voice, String format) {
            return aliyunTtsService.synthesize(text, voice, format);
        }
    }

    @Service
    public static class AliyunAIServiceAdapter implements AIService {
        @Autowired
        private AliyunAIService aliyunAIService;

        @Override
        public String generateAIResponse(String characterName, String characterDescription,
                String personalityTraits, String backgroundStory,
                List<Map<String, Object>> chatHistory) {
            return aliyunAIService.generateAIResponse(characterName, characterDescription,
                    personalityTraits, backgroundStory, chatHistory);
        }
    }

    @Service
    public static class AliyunOSSServiceAdapter implements OssService {
        @Autowired
        private AliyunOSSService aliyunOSSService;

        @Override
        public String uploadFile(MultipartFile file) {
            return aliyunOSSService.uploadFile(file);
        }
    }
}