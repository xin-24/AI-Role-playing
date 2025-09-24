package com.ai.roleplay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ai.roleplay.service.QiniuAsrService;

@RestController
@RequestMapping("/api/asr")
@CrossOrigin(origins = "*")
public class AsrController {

    @Autowired
    private QiniuAsrService qiniuAsrService;

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribe(@RequestParam("file") MultipartFile file) {
        return qiniuAsrService.transcribe(file);
    }
}
