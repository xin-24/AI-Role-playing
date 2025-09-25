package com.ai.roleplay.service;

import com.ai.roleplay.config.QiniuOSSConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class QiniuOSSService {

    private static final Logger logger = LoggerFactory.getLogger(QiniuOSSService.class);

    @Autowired
    private QiniuOSSConfig qiniuOSSConfig;

    /**
     * 上传文件到七牛云对象存储
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file) {
        try {
            // 检查配置是否正确
            if (qiniuOSSConfig.getAccessKey() == null || qiniuOSSConfig.getAccessKey().isEmpty() ||
                    qiniuOSSConfig.getAccessKey().equals("your-real-access-key")) {
                throw new RuntimeException("请在application.properties中配置正确的七牛云Access Key");
            }

            if (qiniuOSSConfig.getSecretKey() == null || qiniuOSSConfig.getSecretKey().isEmpty() ||
                    qiniuOSSConfig.getSecretKey().equals("your-real-secret-key")) {
                throw new RuntimeException("请在application.properties中配置正确的七牛云Secret Key");
            }

            if (qiniuOSSConfig.getAccessKey().equals(qiniuOSSConfig.getSecretKey())) {
                logger.warn("警告：Access Key 和 Secret Key 被设置为相同的值，这通常是不正确的");
            }

            if (qiniuOSSConfig.getBucketName() == null || qiniuOSSConfig.getBucketName().isEmpty()) {
                throw new RuntimeException("请在application.properties中配置正确的七牛云Bucket Name");
            }

            // 构造一个带指定Region对象的配置类
            Configuration cfg = new Configuration(Region.region2()); // 华南地区
            // ...其他参数参考类注释

            UploadManager uploadManager = new UploadManager(cfg);

            // 生成上传凭证，设置文件为公共可读
            Auth auth = Auth.create(qiniuOSSConfig.getAccessKey(), qiniuOSSConfig.getSecretKey());
            String upToken = auth.uploadToken(qiniuOSSConfig.getBucketName(), null, 3600, null, true);

            // 检查token是否生成成功
            if (upToken == null || upToken.isEmpty()) {
                logger.error("无法生成上传凭证，请检查Access Key和Secret Key是否正确");
                throw new RuntimeException("无法生成上传凭证，请检查Access Key和Secret Key是否正确");
            }

            // 生成基于时间戳的文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = ".mp3"; // 默认使用mp3格式
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 使用当前时间作为文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String key = timestamp + fileExtension;

            logger.info("准备上传文件到七牛云: bucket={}, key={}", qiniuOSSConfig.getBucketName(), key);

            // 上传文件
            Response response = uploadManager.put(file.getBytes(), key, upToken);

            // 解析上传成功的结果
            if (response.isOK()) {
                logger.info("文件上传成功: bucket={}, key={}", qiniuOSSConfig.getBucketName(), key);
                // 返回文件访问URL，使用指定的外链域名格式
                return "http://t35596h84.hn-bkt.clouddn.com/" + key;
            } else {
                logger.error("文件上传失败，响应码: {}, 响应体: {}", response.statusCode, response.bodyString());
                throw new RuntimeException("文件上传失败，响应码: " + response.statusCode + ", 响应体: " + response.bodyString());
            }
        } catch (QiniuException ex) {
            logger.error("七牛云上传异常: response={}", ex.response, ex);
            String errorMsg = "文件上传失败";
            if (ex.response != null) {
                try {
                    errorMsg = "文件上传失败: " + ex.response.toString() + " " + ex.response.bodyString();
                    logger.error("详细错误信息: {}", ex.response.bodyString());
                } catch (Exception e) {
                    errorMsg = "文件上传失败: " + ex.getMessage();
                }
            }
            throw new RuntimeException(errorMsg, ex);
        } catch (IOException ex) {
            logger.error("文件读取异常: ", ex);
            throw new RuntimeException("文件读取失败: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("文件上传异常: ", ex);
            throw new RuntimeException("文件上传失败: " + ex.getMessage());
        }
    }
}