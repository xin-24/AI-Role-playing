package com.ai.roleplay;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AliyunTtsTest {

    // 配置参数
    private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");
    private static final String MODEL = "qwen3-tts-flash-realtime";
    private static final String VOICE = "zhixiang";

    public static void main(String[] args) {
        System.out.println("开始测试阿里云TTS服务...");
        System.out.println("API_KEY: " + (API_KEY != null && !API_KEY.isEmpty() ? "已设置" : "未设置"));

        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("错误: 请先设置DASHSCOPE_API_KEY环境变量");
            return;
        }

        try {
            testTtsStreaming();
        } catch (Exception e) {
            System.err.println("TTS测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTtsStreaming() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        FileOutputStream fos = new FileOutputStream("aliyun_tts_test.mp3");

        // 配置回调函数
        ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<SpeechSynthesisResult>() {
            @Override
            public void onEvent(SpeechSynthesisResult result) {
                if (result.getAudioFrame() != null) {
                    try {
                        ByteBuffer audioFrame = result.getAudioFrame();
                        byte[] audioData = new byte[audioFrame.remaining()];
                        audioFrame.get(audioData);
                        fos.write(audioData);
                        System.out.println("收到音频数据: " + audioData.length + " 字节");
                    } catch (Exception e) {
                        System.err.println("处理音频数据时出错: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("语音合成完成");
                try {
                    fos.close();
                } catch (Exception e) {
                    System.err.println("关闭文件时出错: " + e.getMessage());
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                System.err.println("语音合成出错: " + e.getMessage());
                try {
                    fos.close();
                } catch (Exception ex) {
                    System.err.println("关闭文件时出错: " + ex.getMessage());
                }
                latch.countDown();
            }
        };

        // 请求参数
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .voice(VOICE)
                .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);

        // 发送文本
        String text = "你好，这是阿里云TTS服务的测试音频。";
        System.out.println("发送文本: " + text);
        synthesizer.streamingCall(text);
        synthesizer.streamingComplete();

        // 等待合成完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        if (completed) {
            System.out.println("TTS测试完成，音频已保存到 aliyun_tts_test.mp3");
        } else {
            System.err.println("TTS测试超时");
        }

        // 关闭连接
        try {
            // 不需要显式关闭连接，SpeechSynthesizer会在完成时自动关闭
            System.out.println("连接已关闭");
        } catch (Exception e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }
}