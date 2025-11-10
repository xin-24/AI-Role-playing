import React, { useState, useEffect } from 'react';
import { sendMessage, getUserId } from '../api/chat';
import ChatBubble from './ChatBubble';
import EmotionBadge from './EmotionBadge';
import './ChatPage.css';

export default function ChatPage({ character }) {
    const [userId, setUserId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState("");
    const [emotion, setEmotion] = useState("neutral");
    const [suggestion, setSuggestion] = useState("");
    const [compScore, setCompScore] = useState(0);
    const [isSending, setIsSending] = useState(false);

    // 获取用户ID
    useEffect(() => {
        async function fetchUserId() {
            const id = await getUserId();
            setUserId(id);
        }
        fetchUserId();
    }, []);

    async function handleSend() {
        if (!text.trim() || !character || !userId) return;

        // 添加用户消息到聊天记录
        const userMessage = {
            from: 'user',
            text: text,
            emotion: null // 用户消息没有情绪
        };
        setMessages(m => [...m, userMessage]);

        // 清空输入框
        setText('');
        setIsSending(true);

        try {
            // 调用后端API发送消息
            const resp = await sendMessage(text, character.id);

            if (resp.success) {
                // 添加AI回复到聊天记录
                if (resp.aiMessages && resp.aiMessages.length > 0) {
                    // 处理分段的AI回复
                    resp.aiMessages.forEach(aiMsg => {
                        const aiMessage = {
                            from: 'assistant',
                            text: aiMsg.message,
                            emotion: 'neutral'
                        };
                        setMessages(m => [...m, aiMessage]);
                    });
                }

                // 如果有音频数据，自动播放
                if (resp.audioData) {
                    try {
                        const audioBytes = Uint8Array.from(atob(resp.audioData), c => c.charCodeAt(0));
                        const blob = new Blob([audioBytes], { type: 'audio/mpeg' });
                        const url = URL.createObjectURL(blob);
                        const audio = new Audio(url);
                        audio.onended = () => {
                            URL.revokeObjectURL(url);
                        };
                        audio.onerror = () => {
                            URL.revokeObjectURL(url);
                        };
                        await audio.play();
                    } catch (audioError) {
                        console.error('播放TTS音频失败:', audioError);
                    }
                }
            } else {
                // 添加错误消息到聊天记录
                const errorMessage = {
                    from: 'assistant',
                    text: resp.error || "抱歉，消息发送失败，请重试。",
                    emotion: 'neutral'
                };
                setMessages(m => [...m, errorMessage]);
            }
        } catch (error) {
            console.error('发送消息失败:', error);
            // 添加错误消息到聊天记录
            const errorMessage = {
                from: 'assistant',
                text: "抱歉，消息发送失败，请检查网络连接。",
                emotion: 'neutral'
            };
            setMessages(m => [...m, errorMessage]);
        } finally {
            setIsSending(false);
        }
    }

    async function handleSuggestionClick() {
        if (!suggestion) return;

        // 将建议话题设置为输入框内容
        setText(suggestion);

        // 自动发送建议话题
        // 注意：这里我们不直接调用handleSend，而是模拟用户输入后点击发送的过程
    }

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="chat-page">
            {/* 顶部情绪提示条和陪伴分数 */}
            <header className="chat-header">
                <div className="emotion-section">
                    <EmotionBadge emotion={emotion} />
                </div>
                <div className="companionship-score">
                    陪伴值：<span className="score-value">{compScore}</span>/100
                </div>
            </header>

            {/* 消息列表 */}
            <div className="message-list">
                {messages.map((m, idx) => (
                    <ChatBubble
                        key={idx}
                        message={m}
                        isPlaying={m.from === 'assistant' && m.text === '当前播放的消息'}
                    />
                ))}
            </div>

            {/* 建议话题 */}
            {suggestion && (
                <div className="suggestion-section">
                    <button
                        className="suggestion-button"
                        onClick={() => setText(suggestion)}
                        disabled={isSending}
                    >
                        💡 试试这个话题：{suggestion}
                    </button>
                </div>
            )}

            {/* 输入区域 */}
            <footer className="input-area">
                <textarea
                    value={text}
                    onChange={e => setText(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder={`对 ${character.name} 说些什么...`}
                    disabled={isSending}
                    rows="3"
                />
                <div className="input-actions">
                    <button
                        onClick={handleSend}
                        disabled={isSending || !text.trim() || !userId}
                        className="send-button"
                    >
                        {isSending ? '发送中...' : '发送'}
                    </button>
                </div>
            </footer>
        </div>
    );
}