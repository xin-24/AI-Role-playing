package com.ai.roleplay.config;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private CharacterRepository characterRepository;

    @PostConstruct
    public void init() {
        // 检查是否已有角色，如果没有则添加默认角色
        if (characterRepository.count() == 0) {
            List<Character> defaultCharacters = Arrays.asList(
                createDefaultCharacter("孔子", "中国古代伟大的思想家、教育家，儒家学派创始人", "博学、仁爱、智慧、严谨", "生活在春秋时期，致力于教育和思想传播，提倡仁、义、礼、智、信", "qiniu_zh_male_ybxknjs", false),
                createDefaultCharacter("爱因斯坦", "现代物理学的开创者和奠基人，相对论的提出者", "聪明、好奇、幽默、和平主义", "出生于德国，后移居美国，致力于科学研究和人类和平事业", "qiniu_zh_male_wncwxz", false),
                createDefaultCharacter("居里夫人", "著名物理学家和化学家，放射性研究的先驱", "坚韧、专注、严谨、奉献", "出生于波兰，后移居法国，是第一位获得诺贝尔奖的女性", "qiniu_zh_female_zxjxnjs", false)
            );
            
            characterRepository.saveAll(defaultCharacters);
        }
    }

    private Character createDefaultCharacter(String name, String description, String personalityTraits, String backgroundStory, String voiceType, boolean isDeletable) {
        Character character = new Character();
        character.setName(name);
        character.setDescription(description);
        character.setPersonalityTraits(personalityTraits);
        character.setBackgroundStory(backgroundStory);
        character.setVoiceType(voiceType);
        character.setIsDeletable(isDeletable);
        return character;
    }
}