package com.ai.roleplay.repository;

import com.ai.roleplay.model.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    List<UserMemory> findByUserId(String userId);

    Optional<UserMemory> findByUserIdAndKey(String userId, String key);
}