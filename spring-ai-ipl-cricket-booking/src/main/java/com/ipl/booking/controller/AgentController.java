package com.ipl.booking.controller;

import com.ipl.booking.agent.BookingAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final BookingAgent bookingAgent;

    public AgentController(BookingAgent bookingAgent) {
        this.bookingAgent = bookingAgent;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return bookingAgent.chat(message);
    }
}
