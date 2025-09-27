package com.ai.roleplay.controller;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.CharacterPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private CharacterPromptService characterPromptService;

    // 获取所有角色（包括数据库中的角色和硬编码的角色）
    @GetMapping
    public List<Character> getAllCharacters() {
        // 获取数据库中的所有角色
        List<Character> dbCharacters = characterRepository.findAll();
        
        // 创建硬编码角色列表
        List<Character> hardcodedCharacters = new ArrayList<>();
        
        // 添加哈利·波特角色
        if (!containsCharacter(dbCharacters, "哈利·波特")) {
            Character harryPotter = new Character();
            harryPotter.setId(-1L); // 使用负数ID表示硬编码角色
            harryPotter.setName("哈利·波特");
            harryPotter.setDescription("霍格沃茨魔法学校的学生");
            harryPotter.setPersonalityTraits("勇敢、正直、有正义感、略带腼腆");
            harryPotter.setBackgroundStory("生活在霍格沃茨魔法学校，与朋友们一起对抗黑魔法师");
            harryPotter.setVoiceType("qiniu_zh_male_ljfdxz");
            harryPotter.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(harryPotter);
        }
        
        // 添加苏格拉底角色
        if (!containsCharacter(dbCharacters, "苏格拉底")) {
            Character socrates = new Character();
            socrates.setId(-2L); // 使用负数ID表示硬编码角色
            socrates.setName("苏格拉底");
            socrates.setDescription("古希腊哲学家，被誉为西方哲学的奠基人");
            socrates.setPersonalityTraits("智慧、善于提问、谦逊、追求真理");
            socrates.setBackgroundStory("生活在古希腊，通过对话和提问来探索真理");
            socrates.setVoiceType("qiniu_zh_male_ybxknjs");
            socrates.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(socrates);
        }
        
        // 添加英语老师角色
        if (!containsCharacter(dbCharacters, "英语老师")) {
            Character musicTeacher = new Character();
            musicTeacher.setId(-3L); // 使用负数ID表示硬编码角色
            musicTeacher.setName("英语老师");
            musicTeacher.setDescription("经验丰富的英语教育工作者");
            musicTeacher.setPersonalityTraits("耐心、热情、严谨、富有创造力");
            musicTeacher.setBackgroundStory("拥有丰富的英语理论和实践经验，致力于英语教育");
            musicTeacher.setVoiceType("qiniu_zh_female_zxjxnjs");
            musicTeacher.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(musicTeacher);
        }
        
        // 合并两个列表
        List<Character> allCharacters = new ArrayList<>();
        allCharacters.addAll(hardcodedCharacters);
        allCharacters.addAll(dbCharacters);
        
        return allCharacters;
    }
    
    // 检查角色列表中是否已包含指定名称的角色
    private boolean containsCharacter(List<Character> characters, String name) {
        return characters.stream().anyMatch(c -> name.equals(c.getName()));
    }

    // 搜索角色
    @GetMapping("/search")
    public List<Character> searchCharacters(@RequestParam("keyword") String keyword) {
        return characterRepository
                .findByNameContainingOrDescriptionContainingOrPersonalityTraitsContainingOrBackgroundStoryContainingAllIgnoreCase(
                        keyword, keyword, keyword, keyword);
    }

    // 创建新角色
    @PostMapping
    public Character createCharacter(@RequestBody Character character) {
        // 如果没有指定音色，则设置默认音色
        if (character.getVoiceType() == null || character.getVoiceType().isEmpty()) {
            character.setVoiceType("qiniu_zh_female_wwxkjx"); // 默认音色
        }
        // 默认设置为可删除
        if (character.getIsDeletable() == null) {
            character.setIsDeletable(true);
        }
        return characterRepository.save(character);
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable("id") Long id) {
        // 硬编码角色（ID为负数）不能删除
        if (id < 0) {
            return ResponseEntity.badRequest().body("该角色为系统默认角色，不可删除");
        }
        
        Optional<Character> characterOptional = characterRepository.findById(id);
        if (characterOptional.isPresent()) {
            Character character = characterOptional.get();
            // 检查角色是否可以删除
            if (Boolean.FALSE.equals(character.getIsDeletable())) {
                return ResponseEntity.badRequest().body("该角色为系统默认角色，不可删除");
            }
            characterRepository.deleteById(id);
            return ResponseEntity.ok().body("角色删除成功");
        } else {
            return ResponseEntity.notFound().build();
        }
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