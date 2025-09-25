package com.ai.roleplay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QiniuOSSConfig {

    @Value("${oss.qiniu.access-key}")
    private String accessKey;

    @Value("${oss.qiniu.secret-key}")
    private String secretKey;

    @Value("${oss.qiniu.bucket-name}")
    private String bucketName;

    @Value("${oss.qiniu.domain}")
    private String domain;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}