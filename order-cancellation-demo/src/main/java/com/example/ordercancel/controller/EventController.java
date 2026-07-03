package com.example.ordercancel.controller;

import com.example.ordercancel.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/events")
@Slf4j
// CORS is handled globally by SecurityConfig — no @CrossOrigin("*") wildcard here
public class EventController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        // Emitter with 5 minutes timeout
        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.info("SSE connection completed. Removing emitter.");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out. Removing emitter.");
            emitter.complete();
            emitters.remove(emitter);
        });

        emitter.onError((ex) -> {
            log.error("SSE connection error. Removing emitter.", ex);
            emitter.completeWithError(ex);
            emitters.remove(emitter);
        });

        // Send an initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected to Order Cancellation Realtime Stream!"));
        } catch (IOException e) {
            log.error("Error sending connection confirmation", e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Listens for local Spring ApplicationEvents of type OrderEvent and broadcasts them to all SSE clients.
     */
    @EventListener
    public void onOrderEvent(OrderEvent event) {
        log.info("Broadcasting order event: {} - {}", event.getEventType(), event.getMessage());
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-event")
                        .data(event));
            } catch (Exception e) {
                log.warn("Failed to send SSE event to emitter. Marking as dead.", e);
                deadEmitters.add(emitter);
            }
        }
        
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
    }
}
