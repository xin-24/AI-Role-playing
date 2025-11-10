package com.ai.roleplay;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;

public class AliyunTtsDirectTest {
    // 从环境变量获取API密钥
    private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String MODEL = "qwen3-tts-flash";

    public static void main(String[] args) {
        System.out.println("开始测试阿里云TTS服务...");
        System.out.println("API_KEY前缀: "
                + (API_KEY != null && !API_KEY.isEmpty() ? API_KEY.substring(0, Math.min(10, API_KEY.length())) + "..."
                        : "未设置"));

        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("错误: 请先设置DASHSCOPE_API_KEY环境变量");
            return;
        }

        try {
            testTtsDirectCall();
        } catch (Exception e) {
            System.err.println("TTS测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTtsDirectCall() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = BASE_URL + "/services/aigc/multimodal-generation/generation";
            System.out.println("调用URL: " + url);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + API_KEY);
            httpPost.setHeader("Content-Type", "application/json");

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);

            JSONObject input = new JSONObject();
            input.put("text", "你好，这是阿里云TTS服务的测试音频。");
            input.put("voice", "zhixiang");
            input.put("language_type", "Chinese");
            requestBody.put("input", input);

            StringEntity entity = new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // 执行请求
            httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                System.out.println("响应状态码: " + statusCode);

                if (response.getEntity() != null) {
                    byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                    if (responseBytes != null && responseBytes.length > 0) {
                        // 检查是否是音频数据（二进制）还是错误信息（文本JSON）
                        String responseStr = new String(responseBytes);
                        if (responseStr.startsWith("{") && responseStr.contains("\"code\"")) {
                            // 这是JSON错误响应
                            System.out.println("错误响应: " + responseStr);
                        } else {
                            // 这应该是音频数据
                            System.out.println("成功接收音频数据，长度: " + responseBytes.length + " 字节");
                            // 保存音频文件
                            try (FileOutputStream fos = new FileOutputStream("direct_tts_test.mp3")) {
                                fos.write(responseBytes);
                                System.out.println("音频文件已保存为 direct_tts_test.mp3");
                            }
                        }
                    }
                }
                return null;
            });
        }
    }
}