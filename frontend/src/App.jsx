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
    // 语音输入相关
    const [isRecording, setIsRecording] = useState(false);
    const recognitionRef = useRef(null);

    // 获取所有角色
    useEffect(() => {
        fetchCharacters();
        // 初始化Web Speech API
        initSpeechSynthesis();
    }, []);

    // 初始化语音识别（Web Speech API）
    useEffect(() => {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            return; // 浏览器不支持
        }
        const recognition = new SpeechRecognition();
        recognition.lang = 'zh-CN';
        recognition.continuous = true;
        recognition.interimResults = true;

        recognition.onresult = (event) => {
            let interimTranscript = '';
            let finalTranscript = '';
            for (let i = event.resultIndex; i < event.results.length; i++) {
                const transcript = event.results[i][0].transcript;
                if (event.results[i].isFinal) {
                    finalTranscript += transcript;
                } else {
                    interimTranscript += transcript;
                }
            }
            // 将识别文本填充到输入框（保留已有内容）
            if (finalTranscript) {
                setNewMessage(prev => (prev ? prev + ' ' : '') + finalTranscript.trim());
            }
        };

        recognition.onerror = (e) => {
            console.error('Speech recognition error:', e);
            setIsRecording(false);
        };

        recognition.onend = () => {
            setIsRecording(false);
        };

        recognitionRef.current = recognition;

        return () => {
            try {
                recognition.stop();
            } catch (_) { }
        };
    }, []);

    const startRecording = () => {
        if (isRecording) return;
        const recognition = recognitionRef.current;
        if (!recognition) {
            alert('当前浏览器不支持语音输入');
            return;
        }
        try {
            recognition.start();
            setIsRecording(true);
        } catch (e) {
            console.error('start recognition failed', e);
        }
    };

    const stopRecording = () => {
        const recognition = recognitionRef.current;
        if (!recognition) return;
        try {
            recognition.stop();
        } catch (e) {
            console.error('stop recognition failed', e);
        } finally {
            setIsRecording(false);
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

        setIsSending(true);

        // 添加用户消息到界面
        const userMessage = {
            characterId: selectedCharacter.id,
            message: newMessage,
            isUserMessage: true
        };

        // 立即更新界面显示用户消息
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
                // 重新获取聊天历史以包含AI回复
                const historyResponse = await fetch(`http://localhost:8082/api/chat/history/${selectedCharacter.id}`);
                if (historyResponse.ok) {
                    const updatedChatHistory = await historyResponse.json();
                    setChatMessages(updatedChatHistory);
                }
            } else {
                // 如果发送失败，显示错误消息
                const errorMessage = {
                    characterId: selectedCharacter.id,
                    message: "抱歉，消息发送失败，请重试。",
                    isUserMessage: false
                };
                setChatMessages([...updatedMessages, errorMessage]);
            }
        } catch (error) {
            console.error('发送消息失败:', error);
            // 显示错误消息
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

        if ('speechSynthesis' in window) {
            // 停止当前正在播放的语音
            if (isSpeaking) {
                window.speechSynthesis.cancel();
                setIsSpeaking(false);
            }

            // 创建语音对象
            const utterance = new SpeechSynthesisUtterance(message);

            // 设置语音参数
            utterance.rate = 1; // 语速 (0.1 - 10)
            utterance.pitch = 1; // 音调 (0 - 2)
            utterance.volume = 1; // 音量 (0 - 1)

            // 选择合适的语音（优先选择中文语音）
            let selectedVoice = null;
            if (availableVoices.length > 0) {
                // 优先选择中文语音
                selectedVoice = availableVoices.find(voice =>
                    voice.lang.includes('zh') || voice.lang.includes('CN') || voice.lang.includes('TW')
                );

                // 如果没有中文语音，则选择英文语音
                if (!selectedVoice) {
                    selectedVoice = availableVoices.find(voice =>
                        voice.lang.includes('en')
                    );
                }

                // 如果还是没有找到，则使用第一个语音
                if (!selectedVoice) {
                    selectedVoice = availableVoices[0];
                }

                utterance.voice = selectedVoice;
            }

            // 设置事件监听器
            utterance.onstart = () => {
                setIsSpeaking(true);
                console.log('开始播放语音');
            };

            utterance.onend = () => {
                setIsSpeaking(false);
                console.log('语音播放完成');
            };

            utterance.onerror = (event) => {
                setIsSpeaking(false);
                console.error('语音播放失败:', event);
                alert('语音播放失败，请重试');
            };

            // 开始播放
            window.speechSynthesis.speak(utterance);
        } else {
            alert('当前浏览器不支持Web Speech API');
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
                                    <textarea
                                        value={newMessage}
                                        onChange={(e) => setNewMessage(e.target.value)}
                                        onKeyPress={handleKeyPress}
                                        placeholder={isRecording ? `正在语音输入...` : `对 ${selectedCharacter.name} 说些什么...`}
                                        disabled={isSending}
                                    />
                                    <button
                                        type="button"
                                        className={`mic-button ${isRecording ? 'recording' : ''}`}
                                        onClick={isRecording ? stopRecording : startRecording}
                                        title={isRecording ? '停止语音输入' : '开始语音输入'}
                                        disabled={isSending}
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