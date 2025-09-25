package com.ai.roleplay.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QiniuAsrServiceTest {

    @Autowired
    private QiniuAsrService qiniuAsrService;

    @Test
    public void testTranscribeByUrl() {
        // 使用一个测试音频文件URL
        String testAudioUrl = "http://idh.qnaigc.com/voicetest.mp3";
        try {
            String result = qiniuAsrService.transcribeByUrl(testAudioUrl);
            System.out.println("ASR转录结果: " + result);
        } catch (Exception e) {
            System.err.println("ASR转录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}