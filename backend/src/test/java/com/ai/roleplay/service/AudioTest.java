package com.ai.roleplay.service;

import java.io.FileOutputStream;
import java.io.IOException;

public class AudioTest {
    public static void main(String[] args) {
        try {
            // 测试英文TTS
            TextToSpeechService textToSpeechService = new TextToSpeechService();
            byte[] englishAudio = textToSpeechService.textToSpeech("Hello World", "kevin16");
            
            // 保存英文音频到文件
            try (FileOutputStream fos = new FileOutputStream("english_test.wav")) {
                fos.write(englishAudio);
            }
            
            System.out.println("English audio saved to english_test.wav, size: " + englishAudio.length + " bytes");
            
            // 只测试FreeTTS，不再测试已删除的中文TTS
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}