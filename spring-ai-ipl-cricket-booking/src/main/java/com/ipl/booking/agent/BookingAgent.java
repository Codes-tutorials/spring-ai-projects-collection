package com.ipl.booking.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class BookingAgent {

    private final ChatClient chatClient;

    public BookingAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a helpful IPL Cricket Booking Agent.
                        You can check available matches and book tickets for users.
                        Always verify the match ID before booking.
                        If a user asks to book, ask for their name and number of tickets if not provided.
                        """)
                .defaultFunctions("getMatches", "bookTicket")
                .build();
    }

    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
