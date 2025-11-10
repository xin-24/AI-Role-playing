package com.ai.roleplay.utils;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SensitiveFilter {

    private final Set<String> bannedWords = new HashSet<>(Arrays.asList(
            "自杀", "我要死", "轻生", "炸", "恐怖分子", "暴力" // 扩充名单
    ));

    public boolean containsSensitive(String text) {
        if (text == null)
            return false;
        for (String w : bannedWords) {
            if (text.contains(w))
                return true;
        }
        return false;
    }

    public String filterOut(String text) {
        if (text == null)
            return text;
        String result = text;
        for (String w : bannedWords) {
            if (result.contains(w)) {
                result = result.replace(w, "***");
            }
        }
        return result;
    }
}