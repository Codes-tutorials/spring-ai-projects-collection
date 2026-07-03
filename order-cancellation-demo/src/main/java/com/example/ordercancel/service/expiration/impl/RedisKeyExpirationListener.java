package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final OrderService orderService;

    @Autowired
    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer, @Lazy OrderService orderService) {
        super(listenerContainer);
        this.orderService = orderService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("[Redis Expired] Received key expiration event for: {}", expiredKey);

        // Check if the expired key corresponds to our order expiration keys
        if (expiredKey != null && expiredKey.startsWith("order:expire:")) {
            try {
                String orderIdStr = expiredKey.substring("order:expire:".length());
                Long orderId = Long.parseLong(orderIdStr);
                
                log.info("[Redis Expired] Order #{} TTL expired, running cancellation check.", orderId);
                orderService.cancelOrder(orderId, "Redis Key Expiration Event Pub/Sub");
            } catch (NumberFormatException e) {
                log.error("[Redis Expired] Failed to parse order ID from key: {}", expiredKey);
            }
        }
    }
}
