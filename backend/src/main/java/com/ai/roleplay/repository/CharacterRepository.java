package com.ai.roleplay.repository;

import com.ai.roleplay.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    // 根据用户ID查找角色
    List<Character> findByUserId(String userId);

    // 根据用户ID和关键词查找角色
    @Query("SELECT c FROM Character c WHERE c.userId = :userId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Character> findByUserIdAndKeyword(@Param("userId") String userId, @Param("keyword") String keyword);

    @Query("SELECT c FROM Character c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Character> searchCharacters(@Param("keyword") String keyword);

    List<Character> findByNameContainingIgnoreCase(String name);

    // 添加新的查询方法，支持按名称、描述、性格特征或背景故事搜索
    List<Character> findByNameContainingOrDescriptionContainingOrPersonalityTraitsContainingOrBackgroundStoryContainingAllIgnoreCase(
            String name, String description, String personalityTraits, String backgroundStory);

    // 查询不可删除的角色数量
    long countByIsDeletableFalse();
}