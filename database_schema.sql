-- UserMemory 表用于存储用户记忆信息
CREATE TABLE user_memory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64),
  `key` VARCHAR(128),
  `value` TEXT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Conversation 表用于记录每条消息
CREATE TABLE conversation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64),
  character_id BIGINT,
  role VARCHAR(16),
  text TEXT,
  emotion VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以提高查询性能
CREATE INDEX idx_user_memory_user_id ON user_memory(user_id);
CREATE INDEX idx_user_memory_key ON user_memory(`key`);
CREATE INDEX idx_conversation_user_id ON conversation(user_id);
CREATE INDEX idx_conversation_character_id ON conversation(character_id);
CREATE INDEX idx_conversation_created_at ON conversation(created_at);