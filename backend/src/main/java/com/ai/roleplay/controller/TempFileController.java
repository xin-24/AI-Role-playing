package com.ai.roleplay.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/temp")
public class TempFileController {

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serveTempFile(@PathVariable String filename) {
        try {
            // 获取临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            Path filePath = Paths.get(tempDir, "voice_uploads", filename);
            
            // 检查文件是否存在
            if (!filePath.toFile().exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定媒体类型
            MediaType mediaType = getMediaType(filename);
            
            // 返回文件
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private MediaType getMediaType(String filename) {
        if (filename.endsWith(".mp3")) {
            return MediaType.valueOf("audio/mpeg");
        } else if (filename.endsWith(".wav")) {
            return MediaType.valueOf("audio/wav");
        } else if (filename.endsWith(".ogg")) {
            return MediaType.valueOf("audio/ogg");
        } else if (filename.endsWith(".webm")) {
            return MediaType.valueOf("audio/webm");
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}