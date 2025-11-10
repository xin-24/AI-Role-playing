// 存储用户ID
let currentUserId = null;

// 获取用户ID
export async function getUserId() {
    if (currentUserId) {
        return currentUserId;
    }

    try {
        const resp = await fetch('/api/chat/user-id');
        const data = await resp.json();
        currentUserId = data.userId;
        return currentUserId;
    } catch (error) {
        console.error('获取用户ID失败:', error);
        // 生成一个临时ID
        currentUserId = 'temp_' + Date.now();
        return currentUserId;
    }
}

export async function sendMessage(text, characterId) {
    // 获取用户ID
    const userId = await getUserId();

    // 使用能生成TTS音频的端点
    const resp = await fetch('/api/chat/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            userId: userId,
            characterId: characterId,
            message: text,
            isUserMessage: true
        })
    });
    return resp.json(); // returns { success, userMessage, aiMessages, audioData }
}