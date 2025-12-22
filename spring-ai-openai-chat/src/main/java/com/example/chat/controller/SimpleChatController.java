package com.example.chat.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SimpleChatController {

    private final ChatClient chatClient;

    public SimpleChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/chat")
    public Map<String, String> chat(@RequestParam(defaultValue = "Tell me a joke") String msg) {
        String response = chatClient.prompt()
                .user(msg)
                .call()
                .content();

        return Map.of("generation", response);
    }
}
