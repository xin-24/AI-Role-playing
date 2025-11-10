package com.ai.roleplay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LocalStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);

    // 定义本地存储目录
    private static final String LOCAL_STORAGE_DIR = "audio_files";

    /**
     * 将文件存储在本地
     *
     * @param file 要存储的文件
     * @return 文件的本地路径
     */
    public String saveFileLocally(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 创建本地存储目录（如果不存在）
            Path storageDir = Paths.get(LOCAL_STORAGE_DIR);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            // 生成基于时间戳的文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 使用时间戳生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = "audio_" + timestamp + fileExtension;

            // 构建文件路径
            Path filePath = storageDir.resolve(filename);

            // 保存文件
            file.transferTo(filePath.toFile());

            String fileUrl = filePath.toString();
            logger.info("文件保存成功，路径: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            logger.error("文件保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件的绝对路径
     *
     * @param localPath 本地相对路径
     * @return 文件的绝对路径
     */
    public String getAbsoluteFilePath(String localPath) {
        try {
            Path path = Paths.get(localPath);
            if (path.isAbsolute()) {
                return localPath;
            }
            return Paths.get("").toAbsolutePath().resolve(path).toString();
        } catch (Exception e) {
            logger.error("获取文件绝对路径失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件绝对路径失败: " + e.getMessage(), e);
        }
    }
}