# 七牛云TTS服务配置与问题解决

## 问题背景
在AI角色扮演平台中，需要集成七牛云TTS（Text-to-Speech）服务，将文本转换为语音播放。最初实现时遇到了一些问题，包括：
1. 使用了错误的voice_type参数导致"invalid_voice_type"错误
2. 请求方法不当导致URL编码问题
3. 请求体结构不符合七牛云API要求

## 解决方案

### 1. 配置文件修改
修改了`application.properties`文件中的TTS配置：

```properties
# Qiniu TTS Configuration (Text-to-Speech)
tts.qiniu.api-key=********
tts.qiniu.base-url=https://openai.qiniu.com/v1
tts.qiniu.model=tts
tts.qiniu.voice=qiniu_zh_female_wwxkjx
tts.qiniu.format=mp3
```

**关键修改点**：
- 将`voice_type`从`zh-CN-Yunxi`更改为`qiniu_zh_female_wwxkjx`，这是七牛云支持的有效音色类型

### 2. 控制器修改
修改了`TtsController.java`文件，使用POST方法处理请求：

```java
@PostMapping(value = "/speak", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> request) {
    String text = request.get("text");
    String voice = request.get("voice");
    String format = request.get("format");
    
    byte[] audioBytes = ttsService.synthesize(text, voice, format);
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if ("mp3".equalsIgnoreCase(format)) {
        mediaType = MediaType.valueOf("audio/mpeg");
    } else if ("wav".equalsIgnoreCase(format)) {
        mediaType = MediaType.valueOf("audio/wav");
    } else if ("ogg".equalsIgnoreCase(format)) {
        mediaType = MediaType.valueOf("audio/ogg");
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    headers.set("Cache-Control", "no-store");
    return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
}
```

### 3. 服务类修改
修改了`QiniuTtsService.java`文件中的请求体结构：

```java
// 根据用户提供的示例构建请求体
JSONObject body = new JSONObject();

JSONObject audio = new JSONObject();
audio.put("voice_type", useVoice);
audio.put("encoding", useFormat);
audio.put("speed_ratio", 1.0);
body.put("audio", audio);

JSONObject request = new JSONObject();
request.put("text", text);
body.put("request", request);
```

### 4. 前端调用修改
前端使用POST请求发送JSON数据：

```javascript
const resp = await fetch(`http://localhost:8082/api/tts/speak`, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        text: message,
        format: 'mp3'
    }),
    signal: controller.signal
});
```

## 验证结果
通过curl命令测试TTS接口：
```bash
curl -X POST http://localhost:8082/api/tts/speak -H "Content-Type: application/json" -d '{"text": "你好，世界！", "format": "mp3"}' -o test.mp3
```

返回了正确的音频数据，说明TTS服务已经正常工作。

## 总结
通过以上修改，成功解决了七牛云TTS服务的配置问题：
1. 使用了正确的voice_type参数
2. 采用POST方法避免URL编码问题
3. 构建了符合API要求的请求体结构
4. 前端正确调用后端TTS服务并处理返回的音频数据

系统现在能够正常将文本转换为语音播放，并且在后端TTS服务出现问题时会回退到浏览器的Web Speech API，保证了系统的可靠性。