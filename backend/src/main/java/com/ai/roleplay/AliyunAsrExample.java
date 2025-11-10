package com.ai.roleplay;

import com.ai.roleplay.config.AliyunASRConfig;
import com.ai.roleplay.service.AliyunAsrService;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * 阿里云ASR使用示例
 * 
 * 此示例演示如何使用阿里云ASR服务进行语音转文本，而无需配置OSS
 * 音频文件将按时间戳命名存储在本地
 */
public class AliyunAsrExample {

    public static void main(String[] args) {
        // 创建ASR配置
        AliyunASRConfig config = new AliyunASRConfig();
        config.setApiKey(System.getenv("DASHSCOPE_API_KEY")); // 从环境变量获取API Key
        config.setBaseUrl("https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1");
        config.setModel("YOUR_ASR_APPKEY"); // 替换为您的ASR AppKey

        // 创建ASR服务实例
        AliyunAsrService asrService = new AliyunAsrService();

        // 示例：对本地音频文件进行ASR转录
        String audioFilePath = "hello_world.wav"; // 替换为您的音频文件路径

        try {
            // 检查文件是否存在
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                System.out.println("音频文件不存在: " + audioFilePath);
                return;
            }

            // 使用本地文件路径进行ASR转录
            String result = asrService.transcribeByLocalFilePath(audioFilePath);
            System.out.println("ASR转录结果: " + result);
        } catch (Exception e) {
            System.err.println("ASR转录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}