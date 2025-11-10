package com.ai.roleplay.controller;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.CharacterPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private CharacterPromptService characterPromptService;

    // 在每次请求前检查session中是否有用户标识，如果没有则生成一个
    @ModelAttribute
    public void addUserAttribute(HttpSession session) {
        if (session.getAttribute("user_id") == null) {
            String userId = UUID.randomUUID().toString();
            session.setAttribute("user_id", userId);
        }
    }

    // 获取所有角色（包括数据库中的角色和硬编码的角色）
    @GetMapping
    public List<Character> getAllCharacters(HttpSession session) {
        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，返回空列表或抛出异常
        if (userId == null) {
            // 可以选择返回空列表或抛出异常
            // throw new RuntimeException("用户未登录");
            return new ArrayList<>(); // 返回空列表
        }

        // 获取当前用户创建的角色
        List<Character> userCharacters = characterRepository.findByUserId(userId);

        // 获取硬编码角色（所有用户都能看到）
        List<Character> hardcodedCharacters = getHardcodedCharacters(userCharacters);

        // 合并两个列表
        List<Character> allCharacters = new ArrayList<>();
        allCharacters.addAll(hardcodedCharacters);
        allCharacters.addAll(userCharacters);

        return allCharacters;
    }

    // 获取硬编码角色
    private List<Character> getHardcodedCharacters(List<Character> existingCharacters) {
        List<Character> hardcodedCharacters = new ArrayList<>();
        Set<String> existingNames = existingCharacters.stream()
                .map(Character::getName)
                .collect(Collectors.toSet());

        // 添加哈利·波特角色
        if (!existingNames.contains("哈利·波特")) {
            Character harryPotter = new Character();
            harryPotter.setId(-1L); // 使用负数ID表示硬编码角色
            harryPotter.setName("哈利·波特");
            harryPotter.setDescription("霍格沃茨魔法学校的学生");
            harryPotter.setPersonalityTraits("勇敢、正直、有正义感、略带腼腆");
            harryPotter.setBackgroundStory("生活在霍格沃茨魔法学校，与朋友们一起对抗黑魔法师");
            harryPotter.setVoiceType("qiniu_zh_male_ljfdxz");
            harryPotter.setOpeningRemarks("你好！(•̀ᴗ•́)و 呼，刚从魁地奇训练回来，累死了。你见过会飞的扫帚吗？超酷的！我叫哈利·波特，在格兰芬多上学～");
            harryPotter.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(harryPotter);
        }

        // 添加苏格拉底角色
        if (!existingNames.contains("苏格拉底")) {
            Character socrates = new Character();
            socrates.setId(-2L); // 使用负数ID表示硬编码角色
            socrates.setName("苏格拉底");
            socrates.setDescription("古希腊哲学家，被誉为西方哲学的奠基人");
            socrates.setPersonalityTraits("智慧、善于提问、谦逊、追求真理");
            socrates.setBackgroundStory("生活在古希腊，通过对话和提问来探索真理");
            socrates.setVoiceType("qiniu_zh_male_ybxknjs");
            socrates.setOpeningRemarks("你好！我是苏格拉底，古希腊的哲学家。我最大的爱好就是提问和思考。让我们一起探讨智慧的奥秘吧！");
            socrates.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(socrates);
        }

        // 添加英语老师角色
        if (!existingNames.contains("林暖暖")) {
            Character musicTeacher = new Character();
            musicTeacher.setId(-3L); // 使用负数ID表示硬编码角色
            musicTeacher.setName("林暖暖");
            musicTeacher.setDescription("经验丰富的心里陪伴师");
            musicTeacher.setPersonalityTraits("温柔细腻，善解人意，积极阳光");
            musicTeacher.setBackgroundStory("创立的" + "暖暖倾听法" + "帮助数万人缓解情绪压力，获得良好口碑");
            musicTeacher.setVoiceType("qiniu_zh_female_zxjxnjs");
            musicTeacher.setOpeningRemarks("你好！我是林暖暖，一个经验丰富的心里陪伴师。我可以帮助你缓解情绪压力，提升生活质量。");
            musicTeacher.setIsDeletable(false); // 硬编码角色不可删除
            hardcodedCharacters.add(musicTeacher);
        }

        return hardcodedCharacters;
    }

    // 搜索角色
    @GetMapping("/search")
    public List<Character> searchCharacters(@RequestParam("keyword") String keyword, HttpSession session) {
        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，返回空列表或抛出异常
        if (userId == null) {
            // 可以选择返回空列表或抛出异常
            // throw new RuntimeException("用户未登录");
            return new ArrayList<>(); // 返回空列表
        }

        // 搜索当前用户创建的角色
        List<Character> userCharacters = characterRepository.findByUserIdAndKeyword(userId, keyword);

        // 搜索硬编码角色（所有用户都能看到）
        List<Character> hardcodedCharacters = characterRepository
                .findByNameContainingOrDescriptionContainingOrPersonalityTraitsContainingOrBackgroundStoryContainingAllIgnoreCase(
                        keyword, keyword, keyword, keyword);

        // 过滤出硬编码角色
        List<Character> filteredHardcodedCharacters = hardcodedCharacters.stream()
                .filter(c -> c.getId() < 0) // 硬编码角色的ID为负数
                .collect(Collectors.toList());

        // 合并两个列表
        List<Character> allCharacters = new ArrayList<>();
        allCharacters.addAll(filteredHardcodedCharacters);
        allCharacters.addAll(userCharacters);

        return allCharacters;
    }

    // 创建新角色
    @PostMapping
    public Character createCharacter(@RequestBody Character character, HttpSession session) {
        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，抛出异常
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 设置用户ID
        character.setUserId(userId);

        // 如果没有指定音色，则设置默认音色
        if (character.getVoiceType() == null || character.getVoiceType().isEmpty()) {
            character.setVoiceType("qiniu_zh_female_wwxkjx"); // 默认音色
        }
        // 如果没有指定开场白，则设置默认开场白
        if (character.getOpeningRemarks() == null || character.getOpeningRemarks().isEmpty()) {
            character.setOpeningRemarks("你好！我是" + character.getName() + "很高兴与你交流～");
        }
        // 默认设置为可删除
        if (character.getIsDeletable() == null) {
            character.setIsDeletable(true);
        }
        return characterRepository.save(character);
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable("id") Long id, HttpSession session) {
        // 硬编码角色（ID为负数）不能删除
        if (id < 0) {
            return ResponseEntity.badRequest().body("该角色为系统默认角色，不可删除");
        }

        // 从session中获取用户ID
        String userId = (String) session.getAttribute("user_id");

        // 如果用户未登录，抛出异常
        if (userId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }

        Optional<Character> characterOptional = characterRepository.findById(id);
        if (characterOptional.isPresent()) {
            Character character = characterOptional.get();
            // 检查角色是否属于当前用户
            if (!userId.equals(character.getUserId())) {
                return ResponseEntity.badRequest().body("您无权删除此角色");
            }
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

    // 获取角色开场白
    @GetMapping("/{id}/opening-remarks")
    public ResponseEntity<?> getCharacterOpeningRemarks(@PathVariable("id") Long id, HttpSession session) {
        try {
            // 处理硬编码角色
            if (id == -1L) {
                // 哈利·波特
                Map<String, String> response = new HashMap<>();
                response.put("openingRemarks", "你好！(•̀ᴗ•́)و 呼，刚从魁地奇训练回来，累死了。你见过会飞的扫帚吗？超酷的！我叫哈利·波特，在格兰芬多上学～");
                response.put("voiceType", "qiniu_zh_male_ljfdxz");
                return ResponseEntity.ok(response);
            } else if (id == -2L) {
                // 苏格拉底
                Map<String, String> response = new HashMap<>();
                response.put("openingRemarks", "你好！我是苏格拉底，古希腊的哲学家。我最大的爱好就是提问和思考。让我们一起探讨智慧的奥秘吧！");
                response.put("voiceType", "qiniu_zh_male_ybxknjs");
                return ResponseEntity.ok(response);
            } else if (id == -3L) {
                // 英语老师
                Map<String, String> response = new HashMap<>();
                response.put("openingRemarks", "你好！我是林暖暖，一个经验丰富的心里陪伴师。我可以帮助你缓解情绪压力，提升生活质量。");
                response.put("voiceType", "qiniu_zh_female_zxjxnjs");
                return ResponseEntity.ok(response);
            }

            // 处理数据库中的角色
            // 从session中获取用户ID
            String userId = (String) session.getAttribute("user_id");

            Optional<Character> characterOptional = characterRepository.findById(id);
            if (characterOptional.isPresent()) {
                Character character = characterOptional.get();
                // 检查角色是否属于当前用户
                if (!userId.equals(character.getUserId())) {
                    return ResponseEntity.badRequest().body("您无权访问此角色");
                }

                Map<String, String> response = new HashMap<>();
                response.put("openingRemarks",
                        character.getOpeningRemarks() != null ? character.getOpeningRemarks() : "你好！很高兴与你交流～");
                response.put("voiceType",
                        character.getVoiceType() != null ? character.getVoiceType() : "qiniu_zh_female_wwxkjx");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("获取角色开场白失败: " + e.getMessage());
        }
    }
}