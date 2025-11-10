package com.ai.roleplay.controller;

import com.ai.roleplay.service.CloudServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/asr")
@CrossOrigin(origins = "*")
public class AsrController {

    @Autowired
    private CloudServiceProvider cloudServiceProvider;

    /**
     * 专门用于ASR测试的端点 - 使用data字段方式
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribeAudio(@RequestParam("file") MultipartFile file) {
        try {
            // 使用data字段方式直接处理音频文件
            return cloudServiceProvider.getAsrService().transcribeByUrl(file.getOriginalFilename());
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }

    /**
     * 专门用于ASR测试的端点 - 使用URL方式
     */
    @GetMapping("/transcribe")
    public String transcribeAudioUrl(@RequestParam("url") String audioUrl) {
        try {
            // 使用URL方式处理音频文件
            return cloudServiceProvider.getAsrService().transcribeByUrl(audioUrl);
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }
}