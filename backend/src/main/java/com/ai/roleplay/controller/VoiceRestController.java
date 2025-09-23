package com.ai.roleplay.controller;

import com.ai.roleplay.service.TextToSpeechService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class VoiceRestController {

    private final TextToSpeechService textToSpeechService = new TextToSpeechService();

    public byte[] speak(String text, String voice, String language) {
        try {
            // URL解码文本参数
            String decodedText = URLDecoder.decode(text, StandardCharsets.UTF_8);
            
            // 使用英文TTS (FreeTTS实现)
            String voiceName = (voice != null && !voice.isEmpty()) ? voice : "kevin16";
            return textToSpeechService.textToSpeech(decodedText, voiceName);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public String[] getAvailableVoices() {
        return textToSpeechService.getAvailableVoices();
    }
}