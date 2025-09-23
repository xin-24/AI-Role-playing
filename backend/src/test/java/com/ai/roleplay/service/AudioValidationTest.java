package com.ai.roleplay.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AudioValidationTest {
    public static void main(String[] args) {
        try {
            // 测试英文TTS
            TextToSpeechService textToSpeechService = new TextToSpeechService();
            byte[] englishAudio = textToSpeechService.textToSpeech("Hello, this is a test.", "kevin16");
            
            // 保存英文音频到文件
            try (FileOutputStream fos = new FileOutputStream("english_validation_test.wav")) {
                fos.write(englishAudio);
            }
            
            System.out.println("English audio saved to english_validation_test.wav, size: " + englishAudio.length + " bytes");
            
            // 验证WAV文件头
            validateWavHeader(englishAudio, "English");
            
            // 只测试FreeTTS，不再测试已删除的中文TTS
            
            // 测试不同的语音选项
            String[] voices = textToSpeechService.getAvailableVoices();
            System.out.println("Available voices: " + voices.length);
            for (String voice : voices) {
                System.out.println("- " + voice);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void validateWavHeader(byte[] audioData, String language) {
        if (audioData.length < 44) {
            System.out.println(language + " audio data is too short to contain a valid WAV header");
            return;
        }
        
        // 检查RIFF头
        if (audioData[0] == 'R' && audioData[1] == 'I' && audioData[2] == 'F' && audioData[3] == 'F') {
            System.out.println(language + " WAV header: RIFF format detected");
        } else {
            System.out.println(language + " WAV header: Invalid RIFF format");
        }
        
        // 检查WAVE头
        if (audioData[8] == 'W' && audioData[9] == 'A' && audioData[10] == 'V' && audioData[11] == 'E') {
            System.out.println(language + " WAV header: WAVE format detected");
        } else {
            System.out.println(language + " WAV header: Invalid WAVE format");
        }
        
        // 检查fmt chunk
        if (audioData[12] == 'f' && audioData[13] == 'm' && audioData[14] == 't' && audioData[15] == ' ') {
            System.out.println(language + " WAV header: fmt chunk detected");
        } else {
            System.out.println(language + " WAV header: Invalid fmt chunk");
        }
        
        // 检查data chunk
        if (audioData[36] == 'd' && audioData[37] == 'a' && audioData[38] == 't' && audioData[39] == 'a') {
            System.out.println(language + " WAV header: data chunk detected");
        } else {
            System.out.println(language + " WAV header: Invalid data chunk");
        }
    }
}