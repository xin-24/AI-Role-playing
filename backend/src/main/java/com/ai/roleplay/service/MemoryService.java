package com.ai.roleplay.service;

import com.ai.roleplay.model.UserMemory;
import com.ai.roleplay.repository.UserMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    @Autowired
    private UserMemoryRepository repo;

    public void saveOrUpdate(String userId, String key, String value) {
        Optional<UserMemory> opt = repo.findByUserIdAndKey(userId, key);
        UserMemory m = opt.orElseGet(UserMemory::new);
        m.setUserId(userId);
        m.setKey(key);
        m.setValue(value);
        repo.save(m);
    }

    public String read(String userId, String key) {
        return repo.findByUserIdAndKey(userId, key).map(UserMemory::getValue).orElse(null);
    }

    public Map<String, String> readAll(String userId) {
        List<UserMemory> list = repo.findByUserId(userId);
        return list.stream().collect(Collectors.toMap(UserMemory::getKey, UserMemory::getValue));
    }
}