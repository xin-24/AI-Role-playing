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
        voiceType: ''
    });
    const [selectedCharacter, setSelectedCharacter] = useState(null);
    const [chatMessages, setChatMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isSending, setIsSending] = useState(false);
    const [showAddCharacterForm, setShowAddCharacterForm] = useState(false); // 控制添加角色表单的显示
    const chatContainerRef = useRef(null);
    const charactersContainerRef = useRef(null);
    // Web Speech API相关状态
    const [isSpeaking, setIsSpeaking] = useState(false);
    const [availableVoices, setAvailableVoices] = useState([]);
    // 语音输入相关（改为MediaRecorder -> 后端ASR转写）
    const [isRecording, setIsRecording] = useState(false);
    const [isTranscribing, setIsTranscribing] = useState(false);
    const mediaRecorderRef = useRef(null);
    const recordedChunksRef = useRef([]);

    // 获取所有角色
    useEffect(() => {
        fetchCharacters();
        // 初始化Web Speech API
        initSpeechSynthesis();
    }, []);

    const startRecording = async () => {
        if (isRecording || isTranscribing) {
            console.log('录音已在进行中或正在转写中');
            return;
        }

        try {
            console.log('请求麦克风权限...');
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            console.log('已获取麦克风权限');

            // 检查浏览器支持的MIME类型，优先选择MP3或MP4格式以获得更好的兼容性
            const mimeTypes = ['audio/mp4', 'audio/mpeg', 'audio/webm', 'audio/ogg'];
            let mimeType = '';
            for (const type of mimeTypes) {
                if (MediaRecorder.isTypeSupported(type)) {
                    mimeType = type;
                    break;
                }
            }

            console.log('支持的MIME类型:', mimeType);

            const options = mimeType ? { mimeType } : {};
            const mediaRecorder = new MediaRecorder(stream, options);
            recordedChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                console.log('录音数据可用:', event.data.size);
                if (event.data && event.data.size > 0) {
                    recordedChunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = async () => {
                console.log('录音已停止');
                if (recordedChunksRef.current.length === 0) {
                    console.warn('没有录音数据');
                    setIsRecording(false);
                    return;
                }

                // 创建Blob时指定正确的MIME类型
                const blob = new Blob(recordedChunksRef.current, { type: mimeType || 'audio/webm' });
                console.log('录音Blob大小:', blob.size, '类型:', blob.type);

                // 释放麦克风
                stream.getTracks().forEach(t => t.stop());
                await uploadAndTranscribe(blob);
            };

            mediaRecorder.onerror = (event) => {
                console.error('录音错误:', event.error);
                setIsRecording(false);
            };

            mediaRecorderRef.current = mediaRecorder;
            mediaRecorder.start();
            console.log('录音已开始');
            setIsRecording(true);
        } catch (e) {
            console.error('无法开始录音:', e);
            alert('无法访问麦克风，请检查浏览器权限设置: ' + e.message);
            setIsRecording(false);
        }
    };

    const stopRecording = () => {
        const mr = mediaRecorderRef.current;
        if (mr && mr.state !== 'inactive') {
            try {
                console.log('停止录音');
                mr.stop();
            } catch (e) {
                console.error('停止录音失败', e);
            }
        } else {
            console.log('录音器未激活或不存在');
        }
        setIsRecording(false);
    };

    const uploadAndTranscribe = async (blob) => {
        if (blob.size === 0) {
            console.warn('录音文件为空');
            setIsTranscribing(false);
            return;
        }

        setIsTranscribing(true);
        try {
            console.log('开始上传录音文件，大小:', blob.size);
            const form = new FormData();
            // 根据浏览器支持的格式创建合适的文件扩展名
            let extension = 'webm';
            if (blob.type.includes('mp4') || blob.type.includes('mp3') || blob.type.includes('mpeg')) {
                extension = 'mp3';
            } else if (blob.type.includes('ogg')) {
                extension = 'ogg';
            }

            const file = new File([blob], `record.${extension}`, { type: blob.type });
            form.append('file', file);
            form.append('characterId', selectedCharacter.id);

            // 修正API端点URL
            const resp = await fetch('http://localhost:8082/api/voice-chat/send-voice', {
                method: 'POST',
                body: form,
            });

            console.log('ASR响应状态:', resp.status);
            if (!resp.ok) {
                const text = await resp.text();
                console.error('ASR错误响应:', text);
                throw new Error(text || 'ASR服务返回错误');
            }
            const result = await resp.json();
            console.log('ASR识别结果:', result);

            if (result.success) {
                // 设置用户消息
                if (result.transcribedText) {
                    const finalText = result.transcribedText.trim();
                    setNewMessage(prev => (prev ? prev + ' ' : '') + finalText);
                }

                // 获取更新后的聊天历史
                const historyResponse = await fetch(`http://localhost:8082/api/chat/history/${selectedCharacter.id}`);
                if (historyResponse.ok) {
                    const updatedChatHistory = await historyResponse.json();
                    setChatMessages(updatedChatHistory);

                    // 如果有音频数据，自动播放
                    if (result.audioData) {
                        try {
                            const audioBytes = Uint8Array.from(atob(result.audioData), c => c.charCodeAt(0));
                            const blob = new Blob([audioBytes], { type: 'audio/mpeg' });
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
                            setIsSpeaking(true);
                            await audio.play();
                        } catch (audioError) {
                            console.error('播放TTS音频失败:', audioError);
                        }
                    }
                }
            } else {
                throw new Error(result.error || '语音处理失败');
            }
        } catch (err) {
            console.error('转写失败:', err);
            alert('语音转文本失败，请重试: ' + err.message);
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

    // 获取所有音色选项
    const fetchVoiceList = async () => {
        try {
            const response = await fetch('http://localhost:8082/api/characters/voices');
            const data = await response.json();
            setAvailableVoices(data);
        } catch (error) {
            console.error('获取音色列表失败:', error);
            // 使用默认音色列表
            setAvailableVoices([
                { voice_name: "温婉学科讲师", voice_type: "qiniu_zh_female_wwxkjx" },
                { voice_name: "甜美教学小源", voice_type: "qiniu_zh_female_tmjxxy" },
                { voice_name: "校园清新学姐", voice_type: "qiniu_zh_female_xyqxxj" },
                { voice_name: "邻家辅导学长", voice_type: "qiniu_zh_male_ljfdxz" },
                { voice_name: "温和学科小哥", voice_type: "qiniu_zh_male_whxkxg" }
            ]);
        }
    };

    // 预览音色
    const previewVoice = async (voiceType) => {
        if (!voiceType) return;

        try {
            const text = "你好，我是您的AI助手";
            const resp = await fetch(`http://localhost:8082/api/tts/speak`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    text: text,
                    voice: voiceType,
                    format: 'mp3'
                })
            });

            if (!resp.ok) throw new Error(`TTS接口错误: ${resp.status}`);
            const contentType = resp.headers.get('content-type') || '';
            if (!contentType.includes('audio')) throw new Error(`返回非音频类型: ${contentType}`);
            const arrayBuffer = await resp.arrayBuffer();
            if (!arrayBuffer || arrayBuffer.byteLength === 0) throw new Error('音频为空');
            const blob = new Blob([arrayBuffer], { type: contentType });
            const url = URL.createObjectURL(blob);
            const audio = new Audio(url);
            audio.onended = () => URL.revokeObjectURL(url);
            audio.onerror = () => URL.revokeObjectURL(url);
            await audio.play();
        } catch (e) {
            console.error('音色预览失败:', e);
            alert('音色预览失败');
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
        // 如果没有选择音色，使用推荐音色
        const characterData = {
            ...newCharacter,
            voiceType: newCharacter.voiceType || recommendVoice()
        };

        try {
            const response = await fetch('http://localhost:8082/api/characters', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(characterData),
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
                    voiceType: ''
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
                const result = await response.json();
                if (result.success) {
                    // 更新聊天历史
                    const historyResponse = await fetch(`http://localhost:8082/api/chat/history/${selectedCharacter.id}`);
                    if (historyResponse.ok) {
                        const updatedChatHistory = await historyResponse.json();
                        setChatMessages(updatedChatHistory);

                        // 如果有音频数据，自动播放
                        if (result.audioData) {
                            try {
                                const audioBytes = Uint8Array.from(atob(result.audioData), c => c.charCodeAt(0));
                                const blob = new Blob([audioBytes], { type: 'audio/mpeg' });
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
                                setIsSpeaking(true);
                                await audio.play();
                            } catch (audioError) {
                                console.error('播放TTS音频失败:', audioError);
                            }
                        }
                    }
                } else {
                    const errorMessage = {
                        characterId: selectedCharacter.id,
                        message: result.error || "抱歉，消息发送失败，请重试。",
                        isUserMessage: false
                    };
                    setChatMessages([...updatedMessages, errorMessage]);
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
    const playVoice = async (message, characterVoiceType) => {
        if (!message.trim()) return;

        // 优先使用后端TTS（带超时与响应校验）
        try {
            const TTS_REQUEST_TIMEOUT_MS = 10000;
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), TTS_REQUEST_TIMEOUT_MS);
            setIsSpeaking(true);

            // 使用POST请求发送JSON数据，包含角色特定音色
            const resp = await fetch(`http://localhost:8082/api/tts/speak`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    text: message,
                    voice: characterVoiceType, // 使用角色特定音色
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
        if ('speechSynthesis' in window) {
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

    // 推荐音色函数
    const recommendVoice = () => {
        // 这里可以根据角色特征推荐音色，暂时返回默认音色
        return "qiniu_zh_female_wwxkjx";
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
                        <button type="button" onClick={() => setShowAddCharacterForm(true)}>添加角色</button>
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
                    {/* 角色列表 - 固定在左侧 */}
                    <section className="characters-section" ref={charactersContainerRef}>
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
                                    {character.voiceType && (
                                        <p><strong>角色音色:</strong> {character.voiceType}</p>
                                    )}
                                </div>
                            ))}
                        </div>
                    </section>

                    {/* 对话区域 - 固定在右侧 */}
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
                                                        onClick={() => playVoice(msg.message, selectedCharacter.voiceType)}
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

                {/* 添加新角色表单 - 仅在点击添加角色按钮后显示 */}
                {showAddCharacterForm && (
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
                                    name="voiceType"
                                    value={newCharacter.voiceType}
                                    onChange={handleInputChange}
                                >
                                    <option value="">自动推荐音色</option>
                                    <option value="qiniu_zh_female_wwxkjx">温婉学科讲师</option>
                                    <option value="qiniu_zh_female_tmjxxy">甜美教学小源</option>
                                    <option value="qiniu_zh_female_xyqxxj">校园清新学姐</option>
                                    <option value="qiniu_zh_male_ljfdxz">邻家辅导学长</option>
                                    <option value="qiniu_zh_male_whxkxg">温和学科小哥</option>
                                    <option value="qiniu_zh_male_wncwxz">温暖沉稳学长</option>
                                    <option value="qiniu_zh_male_ybxknjs">渊博学科男教师</option>
                                    <option value="qiniu_zh_male_tyygjs">通用阳光讲师</option>
                                    <option value="qiniu_zh_female_glktss">干练课堂思思</option>
                                    <option value="qiniu_zh_female_ljfdxx">邻家辅导学姐</option>
                                    <option value="qiniu_zh_female_kljxdd">开朗教学督导</option>
                                    <option value="qiniu_zh_female_zxjxnjs">知性教学女教师</option>
                                </select>
                                {newCharacter.voiceType && (
                                    <button type="button" onClick={() => previewVoice(newCharacter.voiceType)}>
                                        试听音色
                                    </button>
                                )}
                                {!newCharacter.voiceType && (
                                    <button type="button" onClick={() => previewVoice(recommendVoice())}>
                                        试听推荐音色
                                    </button>
                                )}
                            </div>
                            <div className="form-actions">
                                <button type="submit">添加角色</button>
                                <button type="button" onClick={() => setShowAddCharacterForm(false)}>取消</button>
                            </div>
                        </form>
                    </section>
                )}
            </main>
        </div>
    );
}

export default App;