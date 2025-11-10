# 阿里云服务切换指南

本文档说明如何将项目中的七牛云服务切换为阿里云服务。

## 1. 配置阿里云凭证

### 1.1 设置环境变量
在您的环境中设置阿里云的API密钥：

```bash
echo "export DASHSCOPE_API_KEY='YOUR_DASHSCOPE_API_KEY'" >> ~/.zshrc
source ~/.zshrc
```

### 1.2 更新配置文件
在 `backend/src/main/resources/application.properties` 文件中，更新以下配置项：

```properties
# 云服务提供商切换
cloud.service.provider=aliyun

# Aliyun AI Configuration
llm.aliyun.api-key=${DASHSCOPE_API_KEY}
llm.aliyun.base-url=https://dashscope.aliyuncs.com/api/v1
llm.aliyun.model=qwen-plus

# Aliyun ASR Configuration (Speech-to-Text)
asr.aliyun.api-key=${DASHSCOPE_API_KEY}
asr.aliyun.base-url=https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1
asr.aliyun.model=your-asr-appkey

# Aliyun TTS Configuration (Text-to-Speech)
tts.aliyun.api-key=${DASHSCOPE_API_KEY}
tts.aliyun.base-url=https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1
tts.aliyun.model=your-tts-appkey
tts.aliyun.voice=zhixiang

# Aliyun OSS Configuration (Object Storage)
oss.aliyun.access-key=your-access-key
oss.aliyun.secret-key=your-secret-key
oss.aliyun.bucket-name=your-bucket-name
oss.aliyun.domain=https://your-bucket-name.oss-cn-shanghai.aliyuncs.com
```

## 2. 依赖项

确保在 `backend/pom.xml` 文件中添加了阿里云SDK依赖：

```xml
<!-- Aliyun SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>nls-sdk-java</artifactId>
    <version>2.2.5</version>
</dependency>

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>4.5.30</version>
</dependency>

<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-nls-cloud-meta</artifactId>
    <version>2.0.5</version>
</dependency>

<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.2</version>
</dependency>
```

## 3. 服务切换

项目支持通过配置文件中的 `cloud.service.provider` 参数在七牛云和阿里云之间切换：

- `qiniu`: 使用七牛云服务（默认）
- `aliyun`: 使用阿里云服务

## 4. 服务类说明

### 4.1 阿里云ASR服务
- 类名: `AliyunAsrService`
- 功能: 语音转文字
- 接口: `CloudServiceProvider.AsrService`

### 4.2 阿里云TTS服务
- 类名: `AliyunTtsService`
- 功能: 文字转语音
- 接口: `CloudServiceProvider.TtsService`

### 4.3 阿里云AI服务
- 类名: `AliyunAIService`
- 功能: 大语言模型对话
- 接口: `CloudServiceProvider.AIService`

### 4.4 阿里云OSS服务
- 类名: `AliyunOSSService`
- 功能: 对象存储
- 接口: `CloudServiceProvider.OssService`

## 5. 使用方法

### 5.1 编译项目
```bash
cd backend
./mvnw clean compile
```

### 5.2 运行项目
```bash
cd backend
./mvnw spring-boot:run
```

## 6. 注意事项

1. 请确保您已在阿里云控制台开通相关服务并获取相应的凭证和AppKey。
2. 阿里云的AppKey需要在控制台创建相应的应用后获取。
3. 请根据实际需求调整配置文件中的模型名称和其他参数。
4. 如果遇到网络连接问题，请检查防火墙设置和网络连接。