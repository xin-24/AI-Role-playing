import React from 'react';
import './EmotionBadge.css';

const mapping = {
    sad: { emoji: '😔', text: '你看起来有点难过', color: '#64b5f6' },
    tired: { emoji: '😴', text: '你有点累了，要不要休息一下？', color: '#ba68c8' },
    happy: { emoji: '😊', text: '你看起来很开心呀！', color: '#ffd54f' },
    anxious: { emoji: '😟', text: '有点担心吗？我在这', color: '#e57373' },
    angry: { emoji: '😠', text: '看起来你有些生气呢', color: '#ff8a65' },
    neutral: { emoji: '🙂', text: '最近怎么样？', color: '#90a4ae' }
};

export default function EmotionBadge({ emotion }) {
    const info = mapping[emotion] || mapping['neutral'];
    return (
        <div className="emotion-badge" style={{ borderColor: info.color }}>
            <span className="emotion-emoji">{info.emoji}</span>
            <span className="emotion-text">{info.text}</span>
        </div>
    );
}