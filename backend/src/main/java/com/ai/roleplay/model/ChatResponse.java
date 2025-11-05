package com.ai.roleplay.model;

public class ChatResponse {
    private String text; // LLM 生成的主要回复文本
    private String emotion; // 识别出的情绪标签 e.g. "happy","sad","tired","anxious"
    private String suggestion; // AI给出的下一个话题/互动建议
    private int companionshipScore; // 陪伴成长分（0-100）
    private boolean flagged; // 是否被敏感词过滤标记

    // Constructors
    public ChatResponse() {
    }

    public ChatResponse(String text, String emotion, String suggestion, int companionshipScore, boolean flagged) {
        this.text = text;
        this.emotion = emotion;
        this.suggestion = suggestion;
        this.companionshipScore = companionshipScore;
        this.flagged = flagged;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public int getCompanionshipScore() {
        return companionshipScore;
    }

    public void setCompanionshipScore(int companionshipScore) {
        this.companionshipScore = companionshipScore;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "text='" + text + '\'' +
                ", emotion='" + emotion + '\'' +
                ", suggestion='" + suggestion + '\'' +
                ", companionshipScore=" + companionshipScore +
                ", flagged=" + flagged +
                '}';
    }
}