package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.config.RabbitMQConfig;
import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMQExpirationStrategy implements OrderExpirationStrategy {

    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;

    @Autowired
    public RabbitMQExpirationStrategy(RabbitTemplate rabbitTemplate, @Lazy OrderService orderService) {
        this.rabbitTemplate = rabbitTemplate;
        this.orderService = orderService;
    }

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.RABBITMQ_DLX;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        log.info("[RabbitMQ DLX] Sending order #{} to delay queue with message TTL of {}s", order.getId(), delaySeconds);

        // Send to RabbitMQ delay queue with message TTL
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                order.getId().toString(),
                message -> {
                    // Set expiration in milliseconds
                    message.getMessageProperties().setExpiration(String.valueOf(delaySeconds * 1000));
                    return message;
                }
        );
    }

    /**
     * Listen to the DLQ (Dead Letter Queue). Expired messages will automatically be routed here.
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void consumeExpiredOrder(String orderIdStr) {
        log.info("[RabbitMQ DLX] DLQ Consumer received expired order message: {}", orderIdStr);
        try {
            Long orderId = Long.parseLong(orderIdStr);
            orderService.cancelOrder(orderId, "RabbitMQ TTL + Dead Letter Queue");
        } catch (NumberFormatException e) {
            log.error("[RabbitMQ DLX] Failed to parse order ID from DLQ message: {}", orderIdStr);
        }
    }
}
