import React, { useState, useEffect, useRef } from 'react';
import './App.css';

function App() {
    const [characters, setCharacters] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [newCharacter, setNewCharacter] = useState({
        name: '',
        description: '',
        personalityTraits: '',
        backgroundStory: ''
    });
    const [selectedCharacter, setSelectedCharacter] = useState(null);
    const [chatMessages, setChatMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isSending, setIsSending] = useState(false);
    const chatContainerRef = useRef(null);
    // Web Speech API相关状态
    const [isSpeaking, setIsSpeaking] = useState(false);
    const [availableVoices, setAvailableVoices] = useState([]);
    // 语音输入相关（改为MediaRecorder -> 后端ASR转写）
    const [isRecording, setIsRecording] = useState(false);
    const [isTranscribing, setIsTranscribing] = useState(false);
    const mediaRecorderRef = useRef(null);
    const recordedChunksRef = useRef([]);
    const recognitionRef = useRef(null);

    // 获取所有角色
    useEffect(() => {
        fetchCharacters();
        // 初始化Web Speech API
        initSpeechSynthesis();
    }, []);

    // 不再使用浏览器本地识别，改为MediaRecorder + 后端转写
    useEffect(() => {
        recognitionRef.current = null;
    }, []);

    const startRecording = async () => {
        if (isRecording || isTranscribing) return;
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const options = { mimeType: 'audio/webm' };
            const mediaRecorder = new MediaRecorder(stream, options);
            recordedChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data && event.data.size > 0) {
                    recordedChunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = async () => {
                const blob = new Blob(recordedChunksRef.current, { type: 'audio/webm' });
                // 释放麦克风
                stream.getTracks().forEach(t => t.stop());
                await uploadAndTranscribe(blob);
            };

            mediaRecorderRef.current = mediaRecorder;
            mediaRecorder.start();
            setIsRecording(true);
        } catch (e) {
            console.error('无法开始录音:', e);
            alert('无法访问麦克风，请检查浏览器权限设置');
        }
    };

    const stopRecording = () => {
        const mr = mediaRecorderRef.current;
        if (mr && mr.state !== 'inactive') {
            try {
                mr.stop();
            } catch (e) {
                console.error('停止录音失败', e);
            }
        }
        setIsRecording(false);
    };

    const uploadAndTranscribe = async (blob) => {
        setIsTranscribing(true);
        try {
            const form = new FormData();
            const file = new File([blob], 'record.webm', { type: 'audio/webm' });
            form.append('file', file);
            const resp = await fetch('http://localhost:8082/api/asr/transcribe', {
                method: 'POST',
                body: form,
            });
            if (!resp.ok) {
                const text = await resp.text();
                throw new Error(text || 'ASR服务返回错误');
            }
            const text = await resp.text();
            if (text) {
                const finalText = text.trim();
                setNewMessage(prev => (prev ? prev + ' ' : '') + finalText);
                await sendMessageWithText(finalText);
            }
        } catch (err) {
            console.error('转写失败:', err);
            alert('语音转文本失败，请重试');
        } finally {
            setIsTranscribing(false);
        }
    };

    // 滚动到最新消息
    useEffect(() => {
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [chatMessages]);

    // 初始化Web Speech API
    const initSpeechSynthesis = () => {
        if ('speechSynthesis' in window) {
            // 获取可用的语音列表
            const loadVoices = () => {
                const voices = window.speechSynthesis.getVoices();
                setAvailableVoices(voices);
            };

            // 某些浏览器需要延迟加载语音列表
            if (window.speechSynthesis.onvoiceschanged !== undefined) {
                window.speechSynthesis.onvoiceschanged = loadVoices;
            }

            loadVoices();
        } else {
            console.warn('Web Speech API 不支持当前浏览器');
        }
    };

    const fetchCharacters = async () => {
        try {
            const response = await fetch('http://localhost:8082/api/characters');
            const data = await response.json();
            setCharacters(data);
        } catch (error) {
            console.error('获取角色失败:', error);
        }
    };

    // 搜索角色
    const searchCharacters = async () => {
        if (!searchTerm.trim()) {
            fetchCharacters();
            return;
        }

        try {
            const response = await fetch(`http://localhost:8082/api/characters/search?keyword=${encodeURIComponent(searchTerm)}`);
            const data = await response.json();
            setCharacters(data);
        } catch (error) {
            console.error('搜索角色失败:', error);
        }
    };

    // 创建新角色
    const createCharacter = async () => {
        try {
            const response = await fetch('http://localhost:8082/api/characters', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(newCharacter),
            });

            if (response.ok) {
                const createdCharacter = await response.json();
                setCharacters([...characters, createdCharacter]);
                // 重置表单
                setNewCharacter({
                    name: '',
                    description: '',
                    personalityTraits: '',
                    backgroundStory: ''
                });
            }
        } catch (error) {
            console.error('创建角色失败:', error);
        }
    };

    // 选择角色进行对话
    const selectCharacterForChat = async (character) => {
        setSelectedCharacter(character);
        // 获取聊天历史
        try {
            const response = await fetch(`http://localhost:8082/api/chat/history/${character.id}`);
            if (response.ok) {
                const messages = await response.json();
                setChatMessages(messages);
            }
        } catch (error) {
            console.error('获取聊天历史失败:', error);
            setChatMessages([]);
        }
    };

    // 发送消息
    const sendMessage = async () => {
        if (!newMessage.trim() || !selectedCharacter || isSending) return;

        await sendMessageWithText(newMessage);
    };

    // 直接用指定文本发送（用于ASR转写后自动发送）
    const sendMessageWithText = async (messageText) => {
        const text = (messageText || '').trim();
        if (!text || !selectedCharacter || isSending) return;

        setIsSending(true);

        const userMessage = {
            characterId: selectedCharacter.id,
            message: text,
            isUserMessage: true
        };

        const updatedMessages = [...chatMessages, userMessage];
        setChatMessages(updatedMessages);
        setNewMessage('');

        try {
            const response = await fetch('http://localhost:8082/api/chat/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userMessage),
            });

            if (response.ok) {
                const historyResponse = await fetch(`http://localhost:8082/api/chat/history/${selectedCharacter.id}`);
                if (historyResponse.ok) {
                    const updatedChatHistory = await historyResponse.json();
                    setChatMessages(updatedChatHistory);
                }
            } else {
                const errorMessage = {
                    characterId: selectedCharacter.id,
                    message: "抱歉，消息发送失败，请重试。",
                    isUserMessage: false
                };
                setChatMessages([...updatedMessages, errorMessage]);
            }
        } catch (error) {
            console.error('发送消息失败:', error);
            const errorMessage = {
                characterId: selectedCharacter.id,
                message: "抱歉，消息发送失败，请检查网络连接。",
                isUserMessage: false
            };
            setChatMessages([...updatedMessages, errorMessage]);
        } finally {
            setIsSending(false);
        }
    };

    // 使用Web Speech API播放语音
    const playVoice = async (message) => {
        if (!message.trim()) return;

        // 优先使用后端TTS（带超时与响应校验）
        try {
            const TTS_REQUEST_TIMEOUT_MS = 10000;
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), TTS_REQUEST_TIMEOUT_MS);
            setIsSpeaking(true);
            // 使用POST请求发送JSON数据，避免URL编码问题
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
            clearTimeout(timeoutId);
            if (!resp.ok) throw new Error(`TTS接口错误: ${resp.status}`);
            const contentType = resp.headers.get('content-type') || '';
            if (!contentType.includes('audio')) throw new Error(`返回非音频类型: ${contentType}`);
            const arrayBuffer = await resp.arrayBuffer();
            if (!arrayBuffer || arrayBuffer.byteLength === 0) throw new Error('音频为空');
            const blob = new Blob([arrayBuffer], { type: contentType });
            const url = URL.createObjectURL(blob);
            const audio = new Audio(url);
            audio.onended = () => {
                setIsSpeaking(false);
                URL.revokeObjectURL(url);
            };
            audio.onerror = () => {
                setIsSpeaking(false);
                URL.revokeObjectURL(url);
            };
            await audio.play();
            return; // 成功则不再回退
        } catch (e) {
            console.warn('后端TTS失败，回退到浏览器TTS:', e);
            setIsSpeaking(false);
        }

        // 回退到浏览器SpeechSynthesis
        if ('speechSynthesis' in window) {
            if (isSpeaking) {
                window.speechSynthesis.cancel();
                setIsSpeaking(false);
            }
            const utterance = new SpeechSynthesisUtterance(message);
            utterance.rate = 1;
            utterance.pitch = 1;
            utterance.volume = 1;
            let selectedVoice = null;
            if (availableVoices.length > 0) {
                selectedVoice = availableVoices.find(voice => voice.lang.includes('zh') || voice.lang.includes('CN') || voice.lang.includes('TW'))
                    || availableVoices.find(voice => voice.lang.includes('en'))
                    || availableVoices[0];
                utterance.voice = selectedVoice;
            }
            utterance.onstart = () => setIsSpeaking(true);
            utterance.onend = () => setIsSpeaking(false);
            utterance.onerror = () => setIsSpeaking(false);
            window.speechSynthesis.speak(utterance);
        } else {
            alert('无法播放语音');
        }
    };

    // 停止语音播放
    const stopVoice = () => {
        if ('speechSynthesis' in window && isSpeaking) {
            window.speechSynthesis.cancel();
            setIsSpeaking(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setNewCharacter({
            ...newCharacter,
            [name]: value
        });
    };

    const handleSearchChange = (e) => {
        setSearchTerm(e.target.value);
    };

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        searchCharacters();
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>AI角色扮演平台</h1>
            </header>

            <main>
                {/* 搜索框 */}
                <section className="search-section">
                    <form onSubmit={handleSearchSubmit}>
                        <input
                            type="text"
                            placeholder="搜索角色..."
                            value={searchTerm}
                            onChange={handleSearchChange}
                        />
                        <button type="submit">搜索</button>
                        <button type="button" onClick={fetchCharacters}>显示全部</button>
                    </form>
                </section>

                <div className="main-content">
                    {/* 角色列表 */}
                    <section className="characters-section">
                        <h2>可用角色</h2>
                        <div className="characters-grid">
                            {characters.map((character) => (
                                <div
                                    key={character.id}
                                    className={`character-card ${selectedCharacter && selectedCharacter.id === character.id ? 'selected' : ''}`}
                                    onClick={() => selectCharacterForChat(character)}
                                >
                                    <h3>{character.name}</h3>
                                    <p><strong>描述:</strong> {character.description}</p>
                                    <p><strong>性格特征:</strong> {character.personalityTraits}</p>
                                    <p><strong>背景故事:</strong> {character.backgroundStory}</p>
                                </div>
                            ))}
                        </div>
                    </section>

                    {/* 对话区域 */}
                    {selectedCharacter && (
                        <section className="chat-section">
                            <h2>与 {selectedCharacter.name} 对话</h2>
                            <div className="chat-container">
                                <div className="chat-messages" ref={chatContainerRef}>
                                    {chatMessages.map((msg, index) => (
                                        <div key={index} className={`message ${msg.isUserMessage ? 'user-message' : 'ai-message'}`}>
                                            <div className="message-content">
                                                {msg.message}
                                                {!msg.isUserMessage && (
                                                    <button
                                                        className="voice-button"
                                                        onClick={() => playVoice(msg.message)}
                                                        title={isSpeaking ? "停止播放" : "播放语音"}
                                                        disabled={!msg.message.trim()}
                                                    >
                                                        {isSpeaking ? "⏹️" : "🔊"}
                                                    </button>
                                                )}
                                            </div>
                                            <div className="message-time">
                                                {msg.createdAt ? new Date(msg.createdAt).toLocaleString() : '刚刚'}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                <div className="chat-input">
                                    {isRecording && (
                                        <div className="recording-indicator" title="正在语音输入">
                                            <span className="dot" /> 正在语音输入...
                                        </div>
                                    )}
                                    {isTranscribing && (
                                        <div className="transcribing-indicator" title="正在转写">正在转写...</div>
                                    )}
                                    <textarea
                                        value={newMessage}
                                        onChange={(e) => setNewMessage(e.target.value)}
                                        onKeyPress={handleKeyPress}
                                        placeholder={isRecording ? `正在语音输入...` : (isTranscribing ? '正在转写...' : `对 ${selectedCharacter.name} 说些什么...`)}
                                        disabled={isSending || isTranscribing}
                                    />
                                    <button
                                        type="button"
                                        className={`mic-button ${isRecording ? 'recording' : ''}`}
                                        onClick={isRecording ? stopRecording : startRecording}
                                        title={isRecording ? '停止语音输入' : '开始语音输入'}
                                        disabled={isSending || isTranscribing}
                                    >
                                        {isRecording ? '⏹️' : '🎙️'}
                                    </button>
                                    <button onClick={sendMessage} disabled={isSending}>
                                        {isSending ? '发送中...' : '发送'}
                                    </button>
                                    {/* 添加停止语音按钮 */}
                                    {isSpeaking && (
                                        <button onClick={stopVoice} className="stop-voice-button">
                                            停止语音
                                        </button>
                                    )}
                                </div>
                            </div>
                        </section>
                    )}
                </div>

                {/* 添加新角色表单 */}
                <section className="add-character-section">
                    <h2>添加新角色</h2>
                    <form onSubmit={(e) => {
                        e.preventDefault();
                        createCharacter();
                    }}>
                        <div>
                            <input
                                type="text"
                                name="name"
                                placeholder="角色名称"
                                value={newCharacter.name}
                                onChange={handleInputChange}
                                required
                            />
                        </div>
                        <div>
                            <textarea
                                name="description"
                                placeholder="角色描述"
                                value={newCharacter.description}
                                onChange={handleInputChange}
                                required
                            />
                        </div>
                        <div>
                            <textarea
                                name="personalityTraits"
                                placeholder="性格特征"
                                value={newCharacter.personalityTraits}
                                onChange={handleInputChange}
                                required
                            />
                        </div>
                        <div>
                            <textarea
                                name="backgroundStory"
                                placeholder="背景故事"
                                value={newCharacter.backgroundStory}
                                onChange={handleInputChange}
                                required
                            />
                        </div>
                        <button type="submit">添加角色</button>
                    </form>
                </section>
            </main>
        </div>
    );
}

export default App;