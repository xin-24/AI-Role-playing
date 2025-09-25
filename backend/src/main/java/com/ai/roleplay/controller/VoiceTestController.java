package com.ai.roleplay.controller;

import com.ai.roleplay.service.QiniuAsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/voice-test")
@CrossOrigin(origins = "*")
public class VoiceTestController {

    @Autowired
    private QiniuAsrService qiniuAsrService;

    /**
     * 测试ASR服务是否正常工作
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> testTranscribe(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String transcribedText = qiniuAsrService.transcribe(file);
            
            response.put("success", true);
            response.put("transcribedText", transcribedText);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "转录失败: " + e.getMessage());
        }
        
        return response;
    }
}