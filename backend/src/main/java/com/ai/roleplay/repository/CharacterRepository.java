package com.ai.roleplay.repository;

import com.ai.roleplay.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    @Query("SELECT c FROM Character c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Character> searchCharacters(@Param("keyword") String keyword);

    List<Character> findByNameContainingIgnoreCase(String name);

    // 添加新的查询方法，支持按名称、描述、性格特征或背景故事搜索
    List<Character> findByNameContainingOrDescriptionContainingOrPersonalityTraitsContainingOrBackgroundStoryContainingAllIgnoreCase(
            String name, String description, String personalityTraits, String backgroundStory);
}