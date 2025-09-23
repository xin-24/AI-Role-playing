import React, { useState, useEffect, useRef } from 'react';
import './App.css';

function App() {
    const [characters, setCharacters] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [newCharacter, setNewCharacter] = useState({
        name: '',
        description: '',
        personalityTraits: '',
        backgroundStory: '',
        voiceSettings: ''
    });
    const [selectedCharacter, setSelectedCharacter] = useState(null);
    const [chatMessages, setChatMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isSending, setIsSending] = useState(false);
    const [availableVoices, setAvailableVoices] = useState([]);
    const chatContainerRef = useRef(null);

    // 获取所有角色
    useEffect(() => {
        fetchCharacters();
        fetchAvailableVoices();
    }, []);

    // 滚动到最新消息
    useEffect(() => {
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [chatMessages]);

    const fetchCharacters = async () => {
        try {
            const response = await fetch('http://localhost:8082/api/characters');
            const data = await response.json();
            setCharacters(data);
        } catch (error) {
            console.error('获取角色失败:', error);
        }
    };

    const fetchAvailableVoices = async () => {
        try {
            const response = await fetch('http://localhost:8082/api/characters/voices');
            const voices = await response.json();
            setAvailableVoices(voices);
        } catch (error) {
            console.error('获取可用语音失败:', error);
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
                    backgroundStory: '',
                    voiceSettings: ''
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

    // 播放语音
    const playVoice = async (message) => {
        if (!selectedCharacter) {
            alert('请先选择一个角色');
            return;
        }

        try {
            // 检测消息语言
            const language = detectLanguage(message);

            // 创建一个隐藏的音频元素来播放语音
            const audio = new Audio();

            // 构建URL并处理特殊字符
            const baseUrl = 'http://localhost:8082/api/voice/speak';
            const params = new URLSearchParams();
            params.append('text', message);
            params.append('language', language);

            // 如果有语音设置，则添加
            if (selectedCharacter.voiceSettings) {
                params.append('voice', selectedCharacter.voiceSettings);
            }

            audio.src = `${baseUrl}?${params.toString()}`;

            // 添加事件监听器以处理播放状态
            audio.onended = () => {
                console.log('语音播放完成');
            };

            audio.onerror = (e) => {
                console.error('语音播放失败:', e);
                alert('语音播放失败，请重试');
            };

            // 开始播放
            await audio.play();
        } catch (error) {
            console.error('播放语音失败:', error);
            alert('语音播放失败: ' + error.message);
        }
    };

    /**
     * 检测文本语言
     * 
     * @param {string} text 要检测的文本
     * @return {string} 语言代码 ('zh' 或 'en')
     */
    const detectLanguage = (text) => {
        if (!text) return 'en';

        // 检查是否包含中文字符
        const chineseRegex = /[\u4E00-\u9FFF]/;
        return chineseRegex.test(text) ? 'zh' : 'en';
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
                                    <p><strong>语音设置:</strong> {character.voiceSettings}</p>
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
                                                        title="播放语音"
                                                    >
                                                        🔊
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
                                    <textarea
                                        value={newMessage}
                                        onChange={(e) => setNewMessage(e.target.value)}
                                        onKeyPress={handleKeyPress}
                                        placeholder={`对 ${selectedCharacter.name} 说些什么...`}
                                        disabled={isSending}
                                    />
                                    <button onClick={sendMessage} disabled={isSending}>
                                        {isSending ? '发送中...' : '发送'}
                                    </button>
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
                        <div>
                            <select
                                name="voiceSettings"
                                value={newCharacter.voiceSettings}
                                onChange={handleInputChange}
                                required
                            >
                                <option value="">选择语音</option>
                                {availableVoices.map((voice, index) => (
                                    <option key={index} value={voice}>{voice}</option>
                                ))}
                            </select>
                        </div>
                        <button type="submit">添加角色</button>
                    </form>
                </section>
            </main>
        </div>
    );
}

export default App;