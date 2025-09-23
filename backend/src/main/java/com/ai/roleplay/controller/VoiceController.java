package com.ai.roleplay.controller;

import com.ai.roleplay.service.TextToSpeechService;

import java.util.Map;

public class VoiceController {

    private final TextToSpeechService textToSpeechService = new TextToSpeechService();

    /**
     * 处理语音合成请求（只使用FreeTTS）
     * 
     * @param params 请求参数
     * @return 音频数据字节数组
     */
    public byte[] speak(Map<String, String> params) {
        try {
            String text = params.get("text");
            String voice = params.get("voice");
            
            // 使用英文TTS (FreeTTS实现)
            String voiceName = (voice != null && !voice.isEmpty()) ? voice : "kevin16";
            return textToSpeechService.textToSpeech(text, voiceName);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0]; // 返回空字节数组
        }
    }

    /**
     * 获取可用的语音列表
     * 
     * @return 语音名称数组
     */
    public String[] getAvailableVoices() {
        return textToSpeechService.getAvailableVoices();
    }
}