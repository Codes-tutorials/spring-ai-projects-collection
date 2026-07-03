package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisExpiredExpirationStrategy implements OrderExpirationStrategy {

    private static final String EXPIRE_KEY_PREFIX = "order:expire:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.REDIS_EXPIRED;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        String key = EXPIRE_KEY_PREFIX + order.getId();
        log.info("[Redis Expired] Setting key '{}' with TTL {}s", key, delaySeconds);
        
        // Save the key with the specified expiration time in seconds
        redisTemplate.opsForValue().set(key, order.getId().toString(), delaySeconds, TimeUnit.SECONDS);
    }
}
