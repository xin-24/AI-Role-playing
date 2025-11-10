# 新增功能说明

## 1. 情绪识别服务 (EmotionService)

### 功能描述
情绪识别服务用于分析用户输入文本的情绪，并返回相应的情绪标签。

### 实现方式
- 基于关键词匹配的情绪识别
- 支持 sad, happy, tired, anxious, angry 等情绪
- 可扩展的关键词词典

### 使用示例
```java
@Autowired
private EmotionService emotionService;

// 识别情绪
String emotion = emotionService.detectEmotion("我今天很开心");
// 返回 "happy"
```

## 2. 用户记忆服务 (MemoryService)

### 功能描述
用户记忆服务用于存储和检索用户的偏好信息、最近情绪、上次话题等。

### 数据模型
- UserMemory 实体：存储用户记忆的键值对
- 支持按用户ID和键进行查询

### 使用示例
```java
@Autowired
private MemoryService memoryService;

// 保存或更新用户记忆
memoryService.saveOrUpdate("user123", "favorite_topic", "宠物");

// 读取用户记忆
String favoriteTopic = memoryService.read("user123", "favorite_topic");

// 读取所有用户记忆
Map<String, String> allMemory = memoryService.readAll("user123");
```

## 3. 敏感词过滤 (SensitiveFilter)

### 功能描述
敏感词过滤工具用于检测和过滤用户输入或AI输出中的敏感内容。

### 功能特性
- 检测敏感词
- 替换敏感词为星号
- 可扩展的敏感词列表

### 使用示例
```java
@Autowired
private SensitiveFilter sensitiveFilter;

// 检测是否包含敏感词
boolean hasSensitive = sensitiveFilter.containsSensitive("我想自杀");

// 过滤敏感词
String filtered = sensitiveFilter.filterOut("我想自杀");
// 返回 "我想***"
```

## 4. 增强聊天控制器 (ChatController)

### 功能描述
增强的聊天控制器整合了情绪识别、用户记忆、敏感词过滤等功能。

### API 端点
- POST /api/chat/message - 发送消息并获取情绪化响应

### 响应结构
```json
{
  "text": "回复文本",
  "emotion": "happy",
  "suggestion": "下一个话题建议",
  "companionshipScore": 85,
  "flagged": false
}
```

## 5. 前端增强组件

### ChatPage 组件
- 显示情绪提示条
- 显示陪伴成长进度条
- 显示话题建议
- 支持一键发送建议话题

### EmotionBadge 组件
- 根据情绪显示相应的emoji和文案
- sad: 😔 你看起来有点难过
- happy: 😊 你看起来很开心呀！
- tired: 😴 你有点累了，要不要休息一下？
- anxious: 😟 有点担心吗？我在这
- angry: 😠 看起来你有些生气呢

### ChatBubble 组件
- 根据消息来源和情绪设置不同样式
- 用户消息使用蓝色背景
- AI消息根据情绪使用不同背景色

## 6. 数据库表结构

### user_memory 表
存储用户记忆的键值对信息。

### conversation 表
记录用户与AI的对话历史，包括情绪标签。

## 7. 前端API调用

### 发送消息
```javascript
import { sendMessage } from '../api/chat';

const response = await sendMessage(userId, text, characterId);
// 返回 { text, emotion, suggestion, companionshipScore, flagged }
```

## 8. 安全和隐私

### 敏感词检测
- 检测到自伤/极端内容时返回特殊响应
- 记录敏感事件日志

### 数据存储
- 用户数据仅用于演示目的
- 生产环境需加密存储和隐私条款

### 防滥用
- 对LLM调用异常/返回长文本做截断
- 对请求频率做限流