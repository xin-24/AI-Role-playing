package com.ai.roleplay.controller;

import com.ai.roleplay.service.AliyunAsrService;
import com.ai.roleplay.service.LocalStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/asr-local")
@CrossOrigin(origins = "*")
public class AsrLocalTestController {

    @Autowired
    private AliyunAsrService aliyunAsrService;

    @Autowired
    private LocalStorageService localStorageService;

    /**
     * 上传音频文件并进行ASR转录（存储在本地）
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribeAudio(@RequestParam("file") MultipartFile file) {
        try {
            // 使用本地存储方式进行ASR转录
            return aliyunAsrService.transcribe(file);
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }

    /**
     * 通过本地文件路径进行ASR转录
     */
    @GetMapping("/transcribe")
    public String transcribeAudioByPath(@RequestParam("path") String localFilePath) {
        try {
            // 使用本地文件路径进行ASR转录
            return aliyunAsrService.transcribeByLocalFilePath(localFilePath);
        } catch (Exception e) {
            return "转录失败: " + e.getMessage();
        }
    }
}