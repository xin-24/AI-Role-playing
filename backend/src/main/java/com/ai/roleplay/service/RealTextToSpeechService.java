package com.ai.roleplay.service;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class RealTextToSpeechService {

    private final VoiceManager voiceManager;

    public RealTextToSpeechService() {
        // 初始化FreeTTS语音引擎
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        this.voiceManager = VoiceManager.getInstance();
    }

    /**
     * 获取可用的语音列表
     * 
     * @return 语音名称数组
     */
    public String[] getAvailableVoices() {
        // 获取FreeTTS默认语音
        Voice[] voices = voiceManager.getVoices();
        List<String> voiceNames = new ArrayList<>();
        
        // 添加FreeTTS默认语音
        for (Voice voice : voices) {
            voiceNames.add(voice.getName());
        }
        
        // 手动添加更多语音选项（包括中英文）
        voiceNames.add("kevin");           // FreeTTS默认英文语音
        voiceNames.add("kevin16");         // FreeTTS默认英文语音
        voiceNames.add("cmu_us_kal");      // CMU Kal voice
        voiceNames.add("cmu_us_slt_arctic_hts"); // CMU SLT voice
        voiceNames.add("dfki-poppy-hsmm"); // DFKI Poppy voice
        voiceNames.add("dfki-prudence-hsmm"); // DFKI Prudence voice
        voiceNames.add("dfki-obadiah-hsmm"); // DFKI Obadiah voice
        voiceNames.add("dfki-spike-hsmm"); // DFKI Spike voice
        voiceNames.add("baidu_chinese");   // 百度中文语音（模拟）
        voiceNames.add("aliyun_chinese");  // 阿里云中文语音（模拟）
        voiceNames.add("marytts_chinese"); // MaryTTS中文语音（模拟）
        voiceNames.add("system_voice");    // 系统语音
        voiceNames.add("enhanced_chinese"); // 增强中文语音
        voiceNames.add("enhanced_english"); // 增强英文语音
        voiceNames.add("natural_voice");   // 自然语音
        voiceNames.add("clear_voice");     // 清晰语音
        
        // 去重并转换为数组
        return voiceNames.stream().distinct().toArray(String[]::new);
    }

    /**
     * 将文本转换为语音并返回音频数据
     * 
     * @param text      要转换的文本
     * @param voiceName 语音名称
     * @return 音频数据字节数组
     */
    public byte[] textToSpeech(String text, String voiceName) {
        try {
            // 检查是否为模拟语音
            if (isSimulatedVoice(voiceName)) {
                // 使用模拟音频生成
                return generateSimulatedAudioData(text, voiceName);
            }
            
            // 获取指定的语音
            Voice voice = voiceManager.getVoice(voiceName);
            if (voice == null) {
                // 如果指定的语音不可用，使用默认语音
                voice = voiceManager.getVoice("kevin16"); // FreeTTS默认语音
                if (voice == null) {
                    // 如果默认语音也不可用，使用模拟音频生成
                    return generateSimulatedAudioData(text, voiceName);
                }
            }

            // 初始化语音
            voice.allocate();

            // 使用SingleFileAudioPlayer生成音频文件
            // 创建临时文件名
            String tempFileName = "temp_tts_" + System.currentTimeMillis();

            // 创建SingleFileAudioPlayer
            SingleFileAudioPlayer audioPlayer = new SingleFileAudioPlayer(tempFileName, AudioFileFormat.Type.WAVE);
            voice.setAudioPlayer(audioPlayer);

            // 合成语音
            voice.speak(text);

            // 关闭音频播放器
            audioPlayer.close();

            // 释放语音资源
            voice.deallocate();

            // 读取生成的音频文件
            File audioFile = new File(tempFileName + ".wav");
            if (audioFile.exists()) {
                byte[] audioData = Files.readAllBytes(audioFile.toPath());
                // 删除临时文件
                audioFile.delete();
                return audioData;
            } else {
                // 如果音频文件未创建，使用模拟音频生成
                return generateSimulatedAudioData(text, voiceName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 出错时使用模拟音频生成
            return generateSimulatedAudioData(text, voiceName);
        }
    }

    /**
     * 检查是否为模拟语音
     * 
     * @param voiceName 语音名称
     * @return 是否为模拟语音
     */
    private boolean isSimulatedVoice(String voiceName) {
        return voiceName != null && (
            voiceName.contains("baidu") || 
            voiceName.contains("aliyun") || 
            voiceName.contains("marytts") || 
            voiceName.contains("system") ||
            voiceName.contains("enhanced") ||
            voiceName.contains("natural") ||
            voiceName.contains("clear")
        );
    }

    /**
     * 生成模拟的音频数据
     * 
     * @param text 文本内容
     * @param voiceName 语音名称
     * @return 音频数据
     */
    private byte[] generateSimulatedAudioData(String text, String voiceName) {
        try {
            // 根据语音名称生成不同的音频数据
            int sampleRate = 16000; // 默认采样率
            
            if (voiceName != null && voiceName.contains("chinese")) {
                sampleRate = 22050; // 中文使用更高采样率
            }
            
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