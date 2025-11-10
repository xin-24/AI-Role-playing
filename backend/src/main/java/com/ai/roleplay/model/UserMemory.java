package com.ai.roleplay.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_memory")
public class UserMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId; // 与应用用户 id 绑定

    @Column(name = "`key`")
    private String key; // e.g. "favorite_topic", "pet_name", "last_mood"

    @Column(name = "`value`", length = 5000)
    private String value; // json or plain text

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserMemory() {
    }

    public UserMemory(String userId, String key, String value) {
        this.userId = userId;
        this.key = key;
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    protected void onCreateOrUpdate() {
        updatedAt = LocalDateTime.now();
    }
}