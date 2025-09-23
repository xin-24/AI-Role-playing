package com.ai.roleplay.controller;

import com.ai.roleplay.model.Character;
import com.ai.roleplay.repository.CharacterRepository;
import com.ai.roleplay.service.TextToSpeechService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    @Autowired
    private CharacterRepository characterRepository;

    private final TextToSpeechService textToSpeechService = new TextToSpeechService();

    @GetMapping
    public List<Character> getAllCharacters() {
        return characterRepository.findAll();
    }

    @GetMapping("/{id}")
    public Character getCharacterById(@PathVariable Long id) {
        return characterRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Character createCharacter(@RequestBody Character character) {
        return characterRepository.save(character);
    }

    @GetMapping("/search")
    public List<Character> searchCharacters(@RequestParam String keyword) {
        return characterRepository.searchCharacters(keyword);
    }

    @GetMapping("/voices")
    public String[] getAvailableVoices() {
        return textToSpeechService.getAvailableVoices();
    }
}