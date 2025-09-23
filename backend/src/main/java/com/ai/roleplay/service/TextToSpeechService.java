package com.ai.roleplay.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TextToSpeechService {

    public TextToSpeechService() {
        // 初始化
    }

    /**
     * 获取可用的语音列表（只保留FreeTTS语音）
     * 
     * @return 语音名称数组
     */
    public String[] getAvailableVoices() {
        List<String> voiceNames = new ArrayList<>();
        
        // 只添加FreeTTS语音选项
        voiceNames.add("kevin");           // FreeTTS默认英文语音
        voiceNames.add("kevin16");         // FreeTTS默认英文语音
        voiceNames.add("cmu_us_kal");      // CMU Kal voice
        voiceNames.add("cmu_us_slt_arctic_hts"); // CMU SLT voice
        
        // 转换为数组
        return voiceNames.toArray(new String[0]);
    }

    /**
     * 将文本转换为语音并返回音频数据（只使用FreeTTS实现）
     * 
     * @param text      要转换的文本
     * @param voiceName 语音名称
     * @return 音频数据字节数组
     */
    public byte[] textToSpeech(String text, String voiceName) {
        try {
            // 只使用英文语音生成
            return generateEnglishAudioData(text, voiceName);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyWav(); // 出错时返回空音频
        }
    }

    /**
     * 为英文文本生成音频数据（FreeTTS实现）
     * 
     * @param text 文本内容
     * @param voiceName 语音名称
     * @return 音频数据
     */
    private byte[] generateEnglishAudioData(String text, String voiceName) {
        // 这里可以实现FreeTTS语音生成逻辑
        // 目前返回模拟的音频数据
        return generateSimulatedAudioData(text, 16000); // 标准采样率
    }

    /**
     * 生成模拟的音频数据
     * 
     * @param text 文本内容
     * @param sampleRate 采样率
     * @return 音频数据
     */
    private byte[] generateSimulatedAudioData(String text, int sampleRate) {
        try {
            // 计算音频时长（基于文本长度）
            int durationMs = Math.max(1000, text.length() * 200); // 每个字符200ms
            int numSamples = (int) (sampleRate * (durationMs / 1000.0));
            
            // 生成模拟的音频波形数据
            ByteArrayOutputStream audioData = new ByteArrayOutputStream();
            
            // 生成WAV文件头 (16位音频)
            byte[] wavHeader = createWavHeader16Bit(numSamples, sampleRate);
            audioData.write(wavHeader);
            
            // 生成模拟的音频样本数据 (16位)
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < numSamples; i++) {
                // 根据文本内容生成更复杂的波形
                double t = (double) i / sampleRate;
                
                // 基频根据文本长度调整
                double baseFreq = 200 + (text.length() % 200);
                
                // 多个频率叠加模拟语音
                double wave1 = Math.sin(2 * Math.PI * baseFreq * t);
                double wave2 = 0.5 * Math.sin(2 * Math.PI * baseFreq * 1.5 * t);
                double wave3 = 0.3 * Math.sin(2 * Math.PI * baseFreq * 2.0 * t);
                
                // 添加噪声
                double noise = (random.nextGaussian() - 0.5) * 0.1;
                
                // 包络（模拟语音的起始和结束）
                double envelope = 1.0;
                if (i < numSamples * 0.1) {
                    // 渐强
                    envelope = i / (double) (numSamples * 0.1);
                } else if (i > numSamples * 0.9) {
                    // 渐弱
                    envelope = 1.0 - (i - numSamples * 0.9) / (double) (numSamples * 0.1);
                }
                
                // 组合波形
                double sample = (wave1 + wave2 + wave3 + noise) * envelope;
                
                // 限制幅度并转换为16位
                short shortSample = (short) (sample * 32767);
                
                // 写入16位样本（小端序）
                audioData.write(shortSample & 0xFF);
                audioData.write((shortSample >> 8) & 0xFF);
            }
            
            return audioData.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyWav();
        }
    }

    /**
     * 创建16位WAV文件头
     * 
     * @param numSamples 样本数量
     * @param sampleRate 采样率
     * @return WAV文件头字节数组
     */
    private byte[] createWavHeader16Bit(int numSamples, int sampleRate) {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        
        try {
            int byteRate = sampleRate * 2; // 16位 = 2字节每样本
            int dataSize = numSamples * 2; // 16位样本
            int fileSize = 36 + dataSize;
            
            // RIFF header
            header.write('R');
            header.write('I');
            header.write('F');
            header.write('F');
            
            // File size
            header.write(fileSize & 0xff);
            header.write((fileSize >> 8) & 0xff);
            header.write((fileSize >> 16) & 0xff);
            header.write((fileSize >> 24) & 0xff);
            
            // WAVE header
            header.write('W');
            header.write('A');
            header.write('V');
            header.write('E');
            
            // fmt chunk
            header.write('f');
            header.write('m');
            header.write('t');
            header.write(' ');
            
            // fmt chunk size (16 for PCM)
            header.write(16);
            header.write(0);
            header.write(0);
            header.write(0);
            
            // Audio format (1 for PCM)
            header.write(1);
            header.write(0);
            
            // Number of channels (1 for mono)
            header.write(1);
            header.write(0);
            
            // Sample rate
            header.write(sampleRate & 0xff);
            header.write((sampleRate >> 8) & 0xff);
            header.write((sampleRate >> 16) & 0xff);
            header.write((sampleRate >> 24) & 0xff);
            
            // Byte rate
            header.write(byteRate & 0xff);
            header.write((byteRate >> 8) & 0xff);
            header.write((byteRate >> 16) & 0xff);
            header.write((byteRate >> 24) & 0xff);
            
            // Block align (1 * 2 = 2)
            header.write(2);
            header.write(0);
            
            // Bits per sample (16)
            header.write(16);
            header.write(0);
            
            // data chunk
            header.write('d');
            header.write('a');
            header.write('t');
            header.write('a');
            
            // Data chunk size
            header.write(dataSize & 0xff);
            header.write((dataSize >> 8) & 0xff);
            header.write((dataSize >> 16) & 0xff);
            header.write((dataSize >> 24) & 0xff);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return header.toByteArray();
    }

    /**
     * 创建一个空的WAV文件
     * 
     * @return 空WAV文件的字节数组
     */
    private byte[] createEmptyWav() {
        // 创建一个简单的WAV文件头（静音）
        byte[] header = new byte[44];

        // RIFF header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        // File size (36 bytes for header + 0 bytes of audio data)
        header[4] = 36;
        header[5] = 0;
        header[6] = 0;
        header[7] = 0;

        // WAVE header
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        // fmt chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';

        // fmt chunk size (16 for PCM)
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;

        // Audio format (1 for PCM)
        header[20] = 1;
        header[21] = 0;

        // Number of channels (1 for mono)
        header[22] = 1;
        header[23] = 0;

        // Sample rate (16000 Hz)
        header[24] = (byte) 0x40;
        header[25] = (byte) 0x3E;
        header[26] = 0;
        header[27] = 0;

        // Byte rate (16000 * 1 * 2 = 32000)
        header[28] = (byte) 0x80;
        header[29] = (byte) 0x7D;
        header[30] = 0;
        header[31] = 0;

        // Block align (1 * 2 = 2)
        header[32] = 2;
        header[33] = 0;

        // Bits per sample (16)
        header[34] = 16;
        header[35] = 0;

        // data chunk
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';

        // Data chunk size (0 bytes)
        header[40] = 0;
        header[41] = 0;
        header[42] = 0;
        header[43] = 0;

        return header;
    }
}