package com.ai.roleplay.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "personality_traits", length = 2000)
    private String personalityTraits;

    @Column(name = "background_story", length = 5000)
    private String backgroundStory;

    // 注释掉voiceSettings字段而不是删除，以保持数据库兼容性
    /*
     * @Column(name = "voice_settings")
     * private String voiceSettings;
     */

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Character() {
    }

    public Character(String name, String description, String personalityTraits,
            String backgroundStory/* , String voiceSettings */) {
        this.name = name;
        this.description = description;
        this.personalityTraits = personalityTraits;
        this.backgroundStory = backgroundStory;
        // this.voiceSettings = voiceSettings;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(String personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public String getBackgroundStory() {
        return backgroundStory;
    }

    public void setBackgroundStory(String backgroundStory) {
        this.backgroundStory = backgroundStory;
    }

    // 注释掉voiceSettings的getter和setter方法
    /*
     * public String getVoiceSettings() {
     * return voiceSettings;
     * }
     * 
     * public void setVoiceSettings(String voiceSettings) {
     * this.voiceSettings = voiceSettings;
     * }
     */

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}