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

                {/* 角色列表 */}
                <section className="characters-section">
                    <h2>可用角色</h2>
                    <div className="characters-grid">
                        {characters.map((character) => (
                            <div key={character.id} className="character-card">
                                <h3>{character.name}</h3>
                                <p><strong>描述:</strong> {character.description}</p>
                                <p><strong>性格特征:</strong> {character.personalityTraits}</p>
                                <p><strong>背景故事:</strong> {character.backgroundStory}</p>
                                <p><strong>语音设置:</strong> {character.voiceSettings}</p>
                            </div>
                        ))}
                    </div>
                </section>

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