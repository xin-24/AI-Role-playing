package com.ai.roleplay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.roleplay.service.CloudServiceProvider;

import java.util.Map;

@RestController
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*")
public class TtsController {

    @Autowired
    private CloudServiceProvider cloudServiceProvider;

    @PostMapping(value = "/speak", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String voice = request.get("voice"); // 角色特定音色
        String format = request.get("format");

        // 使用CloudServiceProvider获取相应的TTS服务
        byte[] audioBytes = cloudServiceProvider.getTtsService().synthesize(text, voice, format);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if ("mp3".equalsIgnoreCase(format)) {
            mediaType = MediaType.valueOf("audio/mpeg");
        } else if ("wav".equalsIgnoreCase(format)) {
            mediaType = MediaType.valueOf("audio/wav");
        } else if ("ogg".equalsIgnoreCase(format)) {
            mediaType = MediaType.valueOf("audio/ogg");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("Cache-Control", "no-store");
        return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
    }
}