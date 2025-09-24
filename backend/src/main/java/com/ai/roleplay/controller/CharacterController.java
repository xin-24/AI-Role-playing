package com.ai.roleplay.controller;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    @Autowired
    private CharacterRepository characterRepository;

    // 获取所有角色
    @GetMapping
    public List<Character> getAllCharacters() {
        return characterRepository.findAll();
    }

    // 搜索角色
    @GetMapping("/search")
    public List<Character> searchCharacters(@RequestParam("keyword") String keyword) {
        return characterRepository
                .findByNameContainingOrDescriptionContainingOrPersonalityTraitsContainingOrBackgroundStoryContaining(
                        keyword, keyword, keyword, keyword);
    }

    // 创建新角色
    @PostMapping
    public Character createCharacter(@RequestBody Character character) {
        // 如果没有指定音色，则设置默认音色
        if (character.getVoiceType() == null || character.getVoiceType().isEmpty()) {
            character.setVoiceType("qiniu_zh_female_wwxkjx"); // 默认音色
        }
        return characterRepository.save(character);
    }

    // 获取音色列表
    @GetMapping("/voices")
    public ResponseEntity<?> getVoiceList() {
        try {
            // 返回预定义的音色列表
            Map<String, String>[] voices = new Map[] {
                    createVoiceMap("温婉学科讲师", "qiniu_zh_female_wwxkjx"),
                    createVoiceMap("甜美教学小源", "qiniu_zh_female_tmjxxy"),
                    createVoiceMap("校园清新学姐", "qiniu_zh_female_xyqxxj"),
                    createVoiceMap("邻家辅导学长", "qiniu_zh_male_ljfdxz"),
                    createVoiceMap("温和学科小哥", "qiniu_zh_male_whxkxg"),
                    createVoiceMap("温暖沉稳学长", "qiniu_zh_male_wncwxz"),
                    createVoiceMap("渊博学科男教师", "qiniu_zh_male_ybxknjs"),
                    createVoiceMap("通用阳光讲师", "qiniu_zh_male_tyygjs"),
                    createVoiceMap("干练课堂思思", "qiniu_zh_female_glktss"),
                    createVoiceMap("邻家辅导学姐", "qiniu_zh_female_ljfdxx"),
                    createVoiceMap("开朗教学督导", "qiniu_zh_female_kljxdd"),
                    createVoiceMap("知性教学女教师", "qiniu_zh_female_zxjxnjs")
            };

            return ResponseEntity.ok(voices);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("获取音色列表失败: " + e.getMessage());
        }
    }

    private Map<String, String> createVoiceMap(String name, String type) {
        Map<String, String> voice = new HashMap<>();
        voice.put("voice_name", name);
        voice.put("voice_type", type);
        return voice;
    }
}