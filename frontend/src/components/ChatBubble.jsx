import React from 'react';
import ReactMarkdown from 'react-markdown';
import './ChatBubble.css';

export default function ChatBubble({ message, isPlaying = false }) {
    const isUser = message.from === 'user';

    // 根据情绪设置背景色
    const getBackgroundColor = () => {
        if (isUser) return '#667eea'; // 用户消息使用品牌蓝色

        // AI消息根据情绪设置不同背景色
        switch (message.emotion) {
            case 'happy':
                return '#fff8e1'; // 温暖黄色
            case 'sad':
                return '#e3f2fd'; // 柔和蓝色
            case 'tired':
                return '#f3e5f5'; // 柔和紫色
            case 'anxious':
                return '#ffebee'; // 柔和红色
            case 'angry':
                return '#ffccbc'; // 橙色
            default:
                return '#f5f5f5'; // 默认浅灰色
        }
    };

    // 根据情绪设置边框颜色
    const getBorderColor = () => {
        if (isUser) return '#667eea';

        switch (message.emotion) {
            case 'happy':
                return '#ffd54f';
            case 'sad':
                return '#64b5f6';
            case 'tired':
                return '#ba68c8';
            case 'anxious':
                return '#e57373';
            case 'angry':
                return '#ff8a65';
            default:
                return '#e0e0e0';
        }
    };

    const style = {
        backgroundColor: getBackgroundColor(),
        border: `1px solid ${getBorderColor()}`,
        boxShadow: isPlaying ? '0 0 0 2px #667eea, 0 4px 15px rgba(102, 126, 234, 0.3)' : '0 2px 5px rgba(0, 0, 0, 0.1)'
    };

    // 情绪emoji映射
    const emotionEmojis = {
        happy: '😊',
        sad: '😔',
        tired: '😴',
        anxious: '😟',
        angry: '😠',
        neutral: '🙂'
    };

    return (
        <div className={`chat-bubble ${isUser ? 'user' : 'assistant'} ${isPlaying ? 'playing' : ''}`} style={style}>
            <div className="message-text">
                <ReactMarkdown>{message.text}</ReactMarkdown>
            </div>
            {!isUser && message.emotion && (
                <div className="emotion-indicator">
                    <span className="emotion-emoji">{emotionEmojis[message.emotion] || '🙂'}</span>
                    情绪: {message.emotion}
                </div>
            )}
            {/* 语音播放指示器 */}
            {!isUser && isPlaying && (
                <div className="voice-indicator">
                    <span className="voice-dot"></span>
                    <span className="voice-dot"></span>
                    <span className="voice-dot"></span>
                </div>
            )}
        </div>
    );
}