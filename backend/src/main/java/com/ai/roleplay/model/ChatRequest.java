package com.ai.roleplay.model;

public class ChatRequest {
    private String userId;
    private String text;
    private Long characterId;

    // Constructors
    public ChatRequest() {
    }

    public ChatRequest(String userId, String text, Long characterId) {
        this.userId = userId;
        this.text = text;
        this.characterId = characterId;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
}