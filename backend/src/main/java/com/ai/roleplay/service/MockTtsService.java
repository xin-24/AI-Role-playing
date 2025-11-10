package com.ai.roleplay.service;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class MockTtsService {
    
    /**
     * 生成模拟的TTS音频数据
     * @param text 文本内容
     * @param voice 音色
     * @param format 音频格式
     * @return 模拟的音频数据
     */
    public byte[] synthesize(String text, String voice, String format) {
        System.out.println("使用模拟TTS服务生成音频: text=" + text + ", voice=" + voice + ", format=" + format);
        
        try {
            // 生成一些模拟的音频数据
            // 这里我们生成一个简单的WAV文件头和一些基于文本长度的模拟音频数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV文件头 (44字节)
            baos.write(new byte[] {
                'R', 'I', 'F', 'F',     // RIFF header
                0x24, 0x00, 0x00, 0x00, // 文件大小 (36 + 数据大小)
                'W', 'A', 'V', 'E',     // WAVE header
                'f', 'm', 't', ' ',     // fmt chunk
                0x10, 0x00, 0x00, 0x00, // fmt chunk size (16)
                0x01, 0x00,             // 音频格式 (1 = PCM)
                0x01, 0x00,             // 声道数 (1 = 单声道)
                0x40, 0x1F, 0x00, 0x00, // 采样率 (8000 Hz)
                0x40, 0x1F, 0x00, 0x00, // 每秒字节数 (8000)
                0x01, 0x00,             // 块对齐 (1)
                0x08, 0x00,             // 位深度 (8 bits)
                'd', 'a', 't', 'a',     // data chunk
                0x00, 0x00, 0x00, 0x00  // 数据大小 (将在后面设置)
            });
            
            // 生成模拟音频数据 (基于文本长度和内容)
            int dataLength = Math.max(2000, text.length() * 200); // 至少2000字节，基于文本长度
            Random random = new Random(text.hashCode()); // 使用文本哈希作为种子，使相同文本生成相同音频
            byte[] audioData = new byte[dataLength];
            
            // 生成一些基于文本的模式化数据，使其看起来更像真实的音频
            for (int i = 0; i < dataLength; i++) {
                // 创建一些基于位置和文本的模式
                int pattern = (i * 3 + text.length()) % 256;
                int noise = random.nextInt(50) - 25; // 添加一些随机噪声
                audioData[i] = (byte) ((pattern + noise) & 0xFF);
            }
            
            // 写入音频数据
            baos.write(audioData);
            
            // 更新文件大小字段
            byte[] result = baos.toByteArray();
            int fileSize = result.length - 8;
            int dataSize = result.length - 44;
            
            // 更新文件大小
            result[4] = (byte) (fileSize & 0xFF);
            result[5] = (byte) ((fileSize >> 8) & 0xFF);
            result[6] = (byte) ((fileSize >> 16) & 0xFF);
            result[7] = (byte) ((fileSize >> 24) & 0xFF);
            
            // 更新数据大小
            result[40] = (byte) (dataSize & 0xFF);
            result[41] = (byte) ((dataSize >> 8) & 0xFF);
            result[42] = (byte) ((dataSize >> 16) & 0xFF);
            result[43] = (byte) ((dataSize >> 24) & 0xFF);
            
            System.out.println("模拟TTS服务生成音频数据成功，长度: " + result.length + " 字节");
            return result;
        } catch (Exception e) {
            System.err.println("生成模拟TTS音频失败: " + e.getMessage());
            e.printStackTrace();
            // 如果生成失败，返回一个最小的WAV文件
            return new byte[] {
                'R', 'I', 'F', 'F',
                0x24, 0x00, 0x00, 0x00,
                'W', 'A', 'V', 'E',
                'f', 'm', 't', ' ',
                0x10, 0x00, 0x00, 0x00,
                0x01, 0x00,
                0x01, 0x00,
                0x40, 0x1F, 0x00, 0x00,
                0x40, 0x1F, 0x00, 0x00,
                0x01, 0x00,
                0x08, 0x00,
                'd', 'a', 't', 'a',
                0x00, 0x00, 0x00, 0x00
            };
        }
    }
}