package com.ai.roleplay.service;

import com.ai.roleplay.config.AliyunOSSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class AliyunOSSService {

    private static final Logger logger = LoggerFactory.getLogger(AliyunOSSService.class);

    @Autowired
    private AliyunOSSConfig aliyunOSSConfig;

    /**
     * 上传文件到阿里云OSS
     *
     * @param file 要上传的文件
     * @return 文件的URL
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        OSS ossClient = null;
        try {
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(
                    "oss-cn-shanghai.aliyuncs.com",
                    aliyunOSSConfig.getAccessKey(),
                    aliyunOSSConfig.getSecretKey());

            // 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    aliyunOSSConfig.getBucketName(),
                    uniqueFilename,
                    inputStream);

            // 上传文件
            ossClient.putObject(putObjectRequest);

            // 构建文件URL
            String fileUrl = aliyunOSSConfig.getDomain() + "/" + uniqueFilename;
            logger.info("文件上传成功，URL: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}