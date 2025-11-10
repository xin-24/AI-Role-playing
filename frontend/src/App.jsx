import React, { useState, useEffect, useRef } from 'react';
import './App.css';
import Login from './components/Login';

function App() {
    // 所有状态和引用必须在组件顶层定义
    const [currentUser, setCurrentUser] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [characters, setCharacters] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [newCharacter, setNewCharacter] = useState({
        name: '',
        description: '',
        personalityTraits: '',
        backgroundStory: '',
        voiceType: ''
    });
    const [showAddCharacterForm, setShowAddCharacterForm] = useState(false);
    const [selectedCharacter, setSelectedCharacter] = useState(null);
    const [chatMessages, setChatMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isSending, setIsSending] = useState(false);
    const [isSpeaking, setIsSpeaking] = useState(false);
    const [currentPlayingMessage, setCurrentPlayingMessage] = useState(null);
    const [isFullscreen, setIsFullscreen] = useState(false);
    const chatContainerRef = useRef(null);
    const charactersContainerRef = useRef(null);

    // Web Speech API相关状态
    const [availableVoices, setAvailableVoices] = useState([]);

    // 语音输入相关
    const [isRecording, setIsRecording] = useState(false);
    const [isTranscribing, setIsTranscribing] = useState(false);
    const mediaRecorderRef = useRef(null);
    const recordedChunksRef = useRef([]);

    // 所有useEffect必须在组件顶层定义
    // 检查用户是否已登录
    useEffect(() => {
        checkCurrentUser();
    }, []);

    // 获取所有角色
    useEffect(() => {
        if (currentUser && !isLoading) {
            fetchCharacters();
            // 初始化Web Speech API
            initSpeechSynthesis();
        }
    }, [currentUser, isLoading]);

    // 滚动到最新消息
    useEffect(() => {
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [chatMessages]);

    // 所有函数必须在组件顶层定义
    const checkCurrentUser = async () => {
        try {
            const response = await fetch('/api/auth/me', {
                credentials: 'include'
            });

            if (response.ok) {
                const userData = await response.json();
                setCurrentUser(userData);
            }
        } catch (error) {
            console.error('检查用户登录状态失败:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleLogin = (user) => {
        setCurrentUser(user);
    };

    const handleLogout = async () => {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include'
            });
            setCurrentUser(null);
        } catch (error) {
            console.error('登出失败:', error);
        }
    };

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
            const response = await fetch('/api/characters', {
                credentials: 'include'
            });
            const data = await response.json();
            setCharacters(data);
        } catch (error) {
            console.error('获取角色失败:', error);
        }
    };

    // 获取所有音色选项
    const fetchVoiceList = async () => {
        try {
            const response = await fetch('/api/characters/voices', {
                credentials: 'include'
            });
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
            const text = "你好，欢迎使用FutureBuddy";
            const resp = await fetch('/api/tts/speak', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
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
            const response = await fetch(`/api/characters/search?keyword=${encodeURIComponent(searchTerm)}`, {
                credentials: 'include'
            });
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
            const response = await fetch('/api/characters', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
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
                setShowAddCharacterForm(false);
            }
        } catch (error) {
            console.error('创建角色失败:', error);
        }
    };

    // 删除角色
    const deleteCharacter = async (id) => {
        // 硬编码角色（ID为负数）不能删除
        if (id < 0) {
            alert("该角色为系统默认角色，不可删除");
            return;
        }

        try {
            const response = await fetch(`/api/characters/${id}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                // 从角色列表中移除
                setCharacters(characters.filter(character => character.id !== id));
                // 如果当前选中的角色被删除，取消选择
                if (selectedCharacter && selectedCharacter.id === id) {
                    setSelectedCharacter(null);
                    setChatMessages([]);
                }
                alert("角色删除成功");
            } else {
                const errorData = await response.json();
                alert(errorData.message || '删除角色失败');
            }
        } catch (error) {
            console.error('删除角色失败:', error);
            alert('删除角色失败');
        }
    };

    // 选择角色进行对话
    const selectCharacterForChat = async (character) => {
        setSelectedCharacter(character);
        // 获取聊天历史
        try {
            const response = await fetch(`/api/chat/history/${character.id}`, {
                credentials: 'include'
            });
            if (response.ok) {
                const messages = await response.json();
                setChatMessages(messages);
            }
        } catch (error) {
            console.error('获取聊天历史失败:', error);
            setChatMessages([]);
        }

        // 获取并显示角色开场白
        try {
            const openingResponse = await fetch(`/api/characters/${character.id}/opening-remarks`, {
                credentials: 'include'
            });
            if (openingResponse.ok) {
                const openingData = await openingResponse.json();
                const openingRemarks = openingData.openingRemarks;
                const voiceType = openingData.voiceType;

                if (openingRemarks) {
                    // 创建开场白消息对象
                    const openingMessage = {
                        characterId: character.id,
                        message: openingRemarks,
                        isUserMessage: false,
                        createdAt: new Date().toISOString()
                    };

                    // 添加开场白到聊天记录
                    setChatMessages(prevMessages => [...prevMessages, openingMessage]);

                    // 播放开场白语音
                    if (voiceType) {
                        try {
                            await playVoiceSegment(openingRemarks, voiceType);
                        } catch (error) {
                            console.warn('开场白TTS播放失败:', error);
                        }
                    }
                }
            }
        } catch (error) {
            console.error('获取角色开场白失败:', error);
        }
    };

    const startRecording = async () => {
        if (isRecording || isTranscribing) {
            console.log('录音已在进行中或正在转写中');
            return;
        }

        try {
            console.log('请求麦克风权限...');
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            console.log('已获取麦克风权限');

            // 检查浏览器支持的MIME类型
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
            const resp = await fetch('/api/voice-chat/send-voice', {
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
                const historyResponse = await fetch(`/api/chat/history/${selectedCharacter.id}`, {
                    credentials: 'include'
                });
                if (historyResponse.ok) {
                    const updatedChatHistory = await historyResponse.json();
                    setChatMessages(updatedChatHistory);

                    // 如果有AI回复消息，使用分段显示功能
                    if (result.aiMessages && result.aiMessages.length > 0) {
                        // 移除最后几条AI消息（因为我们要用分段显示替换它们）
                        const messagesWithoutLastAI = updatedChatHistory.slice(0, -result.aiMessages.length);
                        setChatMessages(messagesWithoutLastAI);

                        // 分段显示AI回复
                        await displayAIMessagesInSegments(result.aiMessages, selectedCharacter.id);
                    }

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

    // 发送消息
    const sendMessage = async () => {
        if (!newMessage.trim() || !selectedCharacter) return;

        setIsSending(true);
        const userMessage = {
            characterId: selectedCharacter.id,
            message: newMessage,
            isUserMessage: true
        };

        const updatedMessages = [...chatMessages, userMessage];
        setChatMessages(updatedMessages);
        setNewMessage('');

        try {
            const response = await fetch('/api/chat/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(userMessage),
            });

            if (response.ok) {
                const result = await response.json();
                if (result.success) {
                    // 默认使用流式显示（不再需要检查关键字）
                    if (result.aiMessages && result.aiMessages.length > 0) {
                        // 流式显示AI回复（默认行为）
                        await displayAIMessagesAsStream(result.aiMessages[0].message, selectedCharacter.id);
                    } else {
                        // 更新聊天历史
                        const historyResponse = await fetch(`/api/chat/history/${selectedCharacter.id}`, {
                            credentials: 'include'
                        });
                        if (historyResponse.ok) {
                            const updatedChatHistory = await historyResponse.json();
                            setChatMessages(updatedChatHistory);

                            // 如果有AI回复消息，使用分段显示功能
                            if (result.aiMessages && result.aiMessages.length > 0) {
                                // 移除最后几条AI消息（因为我们要用分段显示替换它们）
                                const messagesWithoutLastAI = updatedChatHistory.slice(0, -result.aiMessages.length);
                                setChatMessages(messagesWithoutLastAI);

                                // 分段显示AI回复
                                await displayAIMessagesInSegments(result.aiMessages, selectedCharacter.id);
                            }

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

    // 将AI回复按字符流式显示
    const displayAIMessagesAsStream = async (fullMessage, characterId) => {
        // 创建临时消息对象
        const tempMessage = {
            characterId: characterId,
            message: '',
            isUserMessage: false,
            createdAt: new Date().toISOString()
        };

        // 先显示空消息
        setChatMessages(prevMessages => [...prevMessages, tempMessage]);

        let currentText = '';
        // 逐字符显示消息
        for (let i = 0; i < fullMessage.length; i++) {
            currentText += fullMessage[i];
            // 更新最后一条消息的内容
            setChatMessages(prevMessages => {
                const newMessages = [...prevMessages];
                newMessages[newMessages.length - 1] = {
                    ...newMessages[newMessages.length - 1],
                    message: currentText
                };
                return newMessages;
            });

            // 添加小延迟以模拟真实流式效果
            await new Promise(resolve => setTimeout(resolve, 30)); // 30ms延迟
        }

        // 保存完整的消息到数据库
        try {
            const saveResponse = await fetch('/api/chat/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    characterId: characterId,
                    message: currentText,
                    isUserMessage: false,
                    createdAt: tempMessage.createdAt // 保持原始创建时间
                }),
            });

            let savedMessage = null;
            if (saveResponse.ok) {
                savedMessage = await saveResponse.json();
                // 更新消息ID
                setChatMessages(prevMessages => {
                    const newMessages = [...prevMessages];
                    newMessages[newMessages.length - 1] = {
                        ...savedMessage,
                        // 确保时间显示正确
                        createdAt: savedMessage.createdAt || tempMessage.createdAt
                    };
                    return newMessages;
                });
            }

            // 播放TTS音频
            const character = characters.find(c => c.id === characterId);
            if (character && currentText) {
                try {
                    await playVoiceSegment(currentText, character.voiceType);
                } catch (error) {
                    console.warn('TTS播放失败:', error);
                }
            }
        } catch (error) {
            console.error('保存消息失败:', error);
        }
    };

    // 将AI回复按标点符号分割成多个片段并依次显示和播放
    const displayAIMessagesInSegments = async (aiMessages, characterId) => {
        // 如果没有消息，直接返回
        if (!aiMessages || aiMessages.length === 0) {
            return;
        }

        // 获取角色信息
        const character = characters.find(c => c.id === characterId);

        // 遍历所有消息片段，按顺序显示和播放
        for (let i = 0; i < aiMessages.length; i++) {
            const messageObj = { ...aiMessages[i] };

            // 显示当前消息（立即显示）
            setChatMessages(prevMessages => [...prevMessages, messageObj]);

            // 如果有角色信息且消息不为空，则播放TTS
            if (character && messageObj.message) {
                try {
                    // 播放当前片段，等待播放完成再继续下一个
                    await playVoiceSegment(messageObj.message, character.voiceType);
                } catch (error) {
                    console.warn('TTS播放失败:', error);
                }
            }
        }
    };

    // 播放单个片段的TTS
    const playVoiceSegment = async (message, characterVoiceType) => {
        if (!message.trim()) return Promise.resolve();

        return new Promise((resolve, reject) => {
            // 设置当前播放的消息
            setCurrentPlayingMessage(message);

            // 优先使用后端TTS（带超时与响应校验）
            const playAudio = (audioData) => {
                try {
                    const blob = new Blob([audioData], { type: 'audio/mpeg' });
                    const url = URL.createObjectURL(blob);
                    const audio = new Audio(url);

                    audio.onended = () => {
                        URL.revokeObjectURL(url);
                        setCurrentPlayingMessage(null); // 清除当前播放的消息
                        resolve();
                    };

                    audio.onerror = (e) => {
                        URL.revokeObjectURL(url);
                        setCurrentPlayingMessage(null); // 清除当前播放的消息
                        reject(new Error('音频播放失败'));
                    };

                    audio.play().catch(reject);
                } catch (e) {
                    setCurrentPlayingMessage(null); // 清除当前播放的消息
                    reject(e);
                }
            };

            const TTS_REQUEST_TIMEOUT_MS = 10000;
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), TTS_REQUEST_TIMEOUT_MS);

            // 使用POST请求发送JSON数据，包含角色特定音色
            fetch(`/api/tts/speak`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    text: message,
                    voice: characterVoiceType || 'Cherry', // 使用角色特定音色，如果未设置则使用默认音色
                    format: 'mp3'
                }),
                signal: controller.signal
            }).then(resp => {
                clearTimeout(timeoutId);
                if (!resp.ok) throw new Error(`TTS接口错误: ${resp.status}`);
                const contentType = resp.headers.get('content-type') || '';
                if (!contentType.includes('audio')) throw new Error(`返回非音频类型: ${contentType}`);
                return resp.arrayBuffer();
            }).then(arrayBuffer => {
                if (!arrayBuffer || arrayBuffer.byteLength === 0) throw new Error('音频为空');
                playAudio(arrayBuffer);
            }).catch(e => {
                console.error('后端TTS失败:', e);
                setCurrentPlayingMessage(null); // 清除当前播放的消息
                // 不再回退到浏览器SpeechSynthesis，直接抛出错误
                reject(new Error('TTS服务不可用: ' + e.message));
            });
        });
    };

    // 使用Web Speech API播放语音
    const playVoice = async (message, characterVoiceType) => {
        if (!message.trim()) return;

        setIsSpeaking(true);

        try {
            await playVoiceSegment(message, characterVoiceType);
        } catch (e) {
            console.error('播放语音失败:', e);
            setIsSpeaking(false);
        }
    };

    // 停止语音播放
    const stopVoice = () => {
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
        }
        setCurrentPlayingMessage(null); // 清除当前播放的消息
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

    // 保存消息到数据库
    const saveMessageToDB = async (characterId, message, isUserMessage, emotion = null, suggestion = null) => {
        try {
            const response = await fetch('/api/chat/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    characterId,
                    message,
                    isUserMessage,
                    emotion,
                    suggestion
                }),
            });

            if (!response.ok) {
                throw new Error(`保存消息失败: ${response.status}`);
            }

            const savedMessage = await response.json();
            return savedMessage;
        } catch (error) {
            console.error('保存消息到数据库失败:', error);
            throw error;
        }
    };

    // 条件渲染必须放在所有Hooks和函数定义之后
    // 显示登录界面，如果用户未登录
    if (isLoading) {
        return <div className="loading">加载中...</div>;
    }

    if (!currentUser) {
        return <Login onLogin={handleLogin} />;
    }

    return (
        <div className="app">
            <div className="cyber-grid"></div>

            <div className="floating-elements">
                <div className="floating-element"></div>
                <div className="floating-element"></div>
                <div className="floating-element"></div>
            </div>

            <header className="app-header">
                <div className="header-glow"></div>
                <div className="header-content">
                    <h1>FutureBuddy - AI角色互动平台</h1>
                    {currentUser && (
                        <div className="user-info">
                            <span>欢迎, {currentUser.username}!</span>
                            <button onClick={handleLogout} className="logout-button">登出</button>
                        </div>
                    )}
                </div>
            </header>

            <main className="app-main">
                <div className="main-content">
                    {/* 角色列表区域 - 仅在未选择角色时显示 */}
                    {!selectedCharacter && (
                        <section className="characters-section">
                            <div className="characters-header">
                                <h2>角色列表</h2>
                                <button
                                    onClick={() => setShowAddCharacterForm(true)}
                                    className="add-character-button"
                                >
                                    + 添加角色
                                </button>
                            </div>

                            {/* 搜索框 */}
                            <form onSubmit={handleSearchSubmit} className="search-form">
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1 }}>
                                    <input
                                        type="text"
                                        value={searchTerm}
                                        onChange={handleSearchChange}
                                        placeholder="搜索角色..."
                                        className="search-input"
                                    />
                                    <button type="submit" className="search-button">搜索</button>
                                </div>
                                {searchTerm && (
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setSearchTerm('');
                                            fetchCharacters();
                                        }}
                                        className="clear-search-button"
                                    >
                                        清除
                                    </button>
                                )}
                            </form>

                            {/* 角色列表 */}
                            <div className="characters-grid" ref={charactersContainerRef}>
                                {characters.map(character => (
                                    <div key={character.id} className="character-card">
                                        <h3>{character.name}</h3>
                                        <p>{character.description}</p>
                                        <div className="character-actions">
                                            <button
                                                onClick={() => selectCharacterForChat(character)}
                                                className="chat-button"
                                            >
                                                开始对话
                                            </button>
                                            <button
                                                onClick={() => deleteCharacter(character.id)}
                                                className="delete-button"
                                            >
                                                删除
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </section>
                    )}

                    {/* 聊天区域 - 仅在选择角色后显示 */}
                    {selectedCharacter && (
                        <section className={`chat-section ${isFullscreen ? 'fullscreen' : ''}`}>
                            <div className="chat-header">
                                <div className="header-glow"></div>
                                <div className="avatar">
                                    {selectedCharacter.name.charAt(0)}
                                </div>
                                <div className="contact-info">
                                    <h2>{selectedCharacter.name}</h2>
                                    <p><span className="status-dot"></span> 在线 - 响应中</p>
                                </div>
                                <div className="header-buttons">
                                    <button
                                        onClick={() => {
                                            setSelectedCharacter(null);
                                            setChatMessages([]);
                                        }}
                                        className="back-button"
                                        title="返回角色列表"
                                    >
                                        ←
                                    </button>
                                    {/* 添加全屏切换按钮 */}
                                    <button
                                        className="fullscreen-toggle-button"
                                        onClick={() => setIsFullscreen(!isFullscreen)}
                                        title={isFullscreen ? "退出全屏" : "全屏显示"}
                                    >
                                        {isFullscreen ? ".EXIT" : "⛶"}
                                    </button>
                                </div>
                            </div>

                            <div className="chat-container" ref={chatContainerRef}>
                                {chatMessages.map((msg, index) => (
                                    <div key={index} className={`message ${msg.isUserMessage ? 'user-message' : 'ai-message'}`}>
                                        <div className={`message-content ${!msg.isUserMessage && currentPlayingMessage === msg.message ? 'playing' : ''}`}>
                                            {msg.message}
                                            {!msg.isUserMessage && (
                                                <>
                                                    {currentPlayingMessage === msg.message && (
                                                        <span className="voice-indicator" title="正在播放语音"></span>
                                                    )}
                                                </>
                                            )}
                                        </div>
                                        <div className="message-time">
                                            {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                            {msg.isUserMessage && (
                                                <span className="message-status">✓✓</span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* 在全屏模式下也保留输入区域 */}
                            <div className="chat-input">
                                {isRecording && (
                                    <div className="recording-indicator" title="正在语音输入">
                                        <span className="dot" /> 正在录音...
                                    </div>
                                )}
                                {isTranscribing && (
                                    <div className="transcribing-indicator" title="正在转写">正在转写...</div>
                                )}
                                <input
                                    type="text"
                                    value={newMessage}
                                    onChange={(e) => setNewMessage(e.target.value)}
                                    onKeyPress={handleKeyPress}
                                    placeholder={isRecording ? `正在录音...` : (isTranscribing ? '正在转写...' : `输入消息...`)}
                                    disabled={isSending || isTranscribing}
                                    className="message-input"
                                />
                                <button onClick={sendMessage} disabled={isSending} className="send-button">
                                    {isSending ? (
                                        <span>⋯</span>
                                    ) : (
                                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <line x1="22" y1="2" x2="11" y2="13"></line>
                                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                                        </svg>
                                    )}
                                </button>
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
                                        🔊 试听音色
                                    </button>
                                )}
                                {!newCharacter.voiceType && (
                                    <button type="button" onClick={() => previewVoice(recommendVoice())}>
                                        🔊 试听推荐音色
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