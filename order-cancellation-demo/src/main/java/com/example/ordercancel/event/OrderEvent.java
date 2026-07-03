package com.example.ordercancel.event;

import com.example.ordercancel.model.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderEvent extends ApplicationEvent {
    private final Order order;
    private final String eventType; // "CREATED", "PAID", "CANCELLED", "EXPIRED_CHECK"
    private final String message;

    public OrderEvent(Object source, Order order, String eventType, String message) {
        super(source);
        this.order = order;
        this.eventType = eventType;
        this.message = message;
    }
}
