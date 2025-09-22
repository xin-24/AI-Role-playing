import React, { useState, useEffect } from 'react';
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

    // 获取所有角色
    useEffect(() => {
        fetchCharacters();
    }, []);

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
        if (!newMessage.trim() || !selectedCharacter) return;

        // 添加用户消息到界面
        const userMessage = {
            characterId: selectedCharacter.id,
            message: newMessage,
            isUserMessage: true
        };

        setChatMessages([...chatMessages, userMessage]);
        
        try {
            const response = await fetch('http://localhost:8082/api/chat/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userMessage),
            });

            if (response.ok) {
                // 这里可以添加AI回复的逻辑
                // 暂时我们只保存用户的消息
                setNewMessage('');
            }
        } catch (error) {
            console.error('发送消息失败:', error);
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
                                <div className="chat-messages">
                                    {chatMessages.map((msg, index) => (
                                        <div key={index} className={`message ${msg.isUserMessage ? 'user-message' : 'ai-message'}`}>
                                            <div className="message-content">
                                                {msg.message}
                                            </div>
                                            <div className="message-time">
                                                {msg.createdAt && new Date(msg.createdAt).toLocaleString()}
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
                                    />
                                    <button onClick={sendMessage}>发送</button>
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
                            <input
                                type="text"
                                name="voiceSettings"
                                placeholder="语音设置"
                                value={newCharacter.voiceSettings}
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