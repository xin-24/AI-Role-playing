package com.ai.roleplay.service;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.roleplay.config.AliyunTTSConfig;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;

@Service
public class AliyunTtsStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(AliyunTtsStreamingService.class);

    @Autowired
    private AliyunTTSConfig ttsConfig;

    public byte[] synthesize(String text, String voice, String format) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("TTS文本不能为空");
        }
        
        String useVoice = voice != null && !voice.isEmpty() ? voice : ttsConfig.getDefaultVoice();
        String useFormat = format != null && !format.isEmpty() ? format : ttsConfig.getDefaultFormat();

        // 将文本分割成小段以便流式处理
        String[] textArray = splitTextIntoSegments(text);

        ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
        CountDownLatch completionLatch = new CountDownLatch(1);

        try {
            // 配置回调函数
            ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<SpeechSynthesisResult>() {
                @Override
                public void onEvent(SpeechSynthesisResult result) {
                    if (result.getAudioFrame() != null) {
                        try {
                            // 将ByteBuffer转换为byte[]
                            byte[] audioData = new byte[result.getAudioFrame().remaining()];
                            result.getAudioFrame().get(audioData);
                            audioStream.write(audioData);
                            logger.debug("收到音频数据");
                        } catch (Exception e) {
                            logger.error("处理音频数据时出错: " + e.getMessage(), e);
                        }
                    }
                }

                @Override
                public void onComplete() {
                    logger.info("语音合成结束");
                    completionLatch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    logger.error("语音合成出错: " + e.getMessage(), e);
                    completionLatch.countDown();
                }
            };

            // 确定音频格式
            SpeechSynthesisAudioFormat audioFormat = SpeechSynthesisAudioFormat.PCM_22050HZ_MONO_16BIT;
            if ("mp3".equalsIgnoreCase(useFormat)) {
                // 使用正确的MP3格式
                audioFormat = SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS;
            } else if ("wav".equalsIgnoreCase(useFormat)) {
                audioFormat = SpeechSynthesisAudioFormat.PCM_22050HZ_MONO_16BIT;
            }

            // 请求参数
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .apiKey(ttsConfig.getApiKey())
                    .model(ttsConfig.getModel())
                    .voice(useVoice)
                    .format(audioFormat)
                    .build();

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);

            // 发送文本片段
            for (String segment : textArray) {
                synthesizer.streamingCall(segment);
            }

            // 结束流式合成
            synthesizer.streamingComplete();

            // 等待合成完成（最多等待30秒）
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("语音合成超时");
            }

            // 关闭连接
            try {
                // 不需要显式关闭连接，SpeechSynthesizer会在完成时自动处理
                logger.info("语音合成完成，连接将自动关闭");
            } catch (Exception e) {
                logger.warn("关闭连接时出错: " + e.getMessage());
            }

            logger.info("[Metric] requestId为：" + synthesizer.getLastRequestId());

            return audioStream.toByteArray();
        } catch (Exception e) {
            logger.error("调用TTS服务失败: " + e.getMessage(), e);
            throw new RuntimeException("调用TTS服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将文本分割成适合流式处理的小段
     */
    private String[] splitTextIntoSegments(String text) {
        // 简单按句号分割，实际应用中可以根据标点符号更智能地分割
        if (text.length() <= 50) {
            return new String[]{text};
        }

        // 按句号分割，每段不超过50个字符
        return text.split("(?<=[。！？\\\\.!?])");
    }
}