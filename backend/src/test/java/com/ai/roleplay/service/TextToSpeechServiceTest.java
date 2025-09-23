package com.ai.roleplay.service;

public class TextToSpeechServiceTest {
    public static void main(String[] args) {
        TextToSpeechService service = new TextToSpeechService();
        String[] voices = service.getAvailableVoices();
        
        System.out.println("Available voices:");
        for (String voice : voices) {
            System.out.println("- " + voice);
        }
        
        System.out.println("\nTotal voices: " + voices.length);
    }
}