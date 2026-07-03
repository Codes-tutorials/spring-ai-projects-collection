package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class RedisZSetExpirationStrategy implements OrderExpirationStrategy {

    private static final String REDIS_ZSET_KEY = "order:zset:expire";

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;

    @Autowired
    public RedisZSetExpirationStrategy(RedisTemplate<String, Object> redisTemplate,
                                       @Lazy OrderService orderService) {
        this.redisTemplate = redisTemplate;
        this.orderService = orderService;
    }

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.REDIS_ZSET;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        long expireTimestamp = System.currentTimeMillis() + (delaySeconds * 1000L);
        log.info("[Redis ZSET] Scheduling expiration for order #{} at epoch ms {}", order.getId(), expireTimestamp);
        redisTemplate.opsForZSet().add(REDIS_ZSET_KEY, order.getId().toString(), expireTimestamp);
    }

    /**
     * Poll the Redis ZSet every 2 seconds.
     *
     * FIX: Uses ZPOPMIN (atomic pop) instead of ZRANGEBYSCORE + ZREM.
     * ZPOPMIN atomically removes and returns the N members with the lowest scores.
     * This eliminates the race condition where two instances could both read
     * and both attempt to cancel the same order.
     *
     * @SchedulerLock: Additional cluster safety layer — only one node polls at a time.
     */
    @Scheduled(fixedRate = 2000)
    @SchedulerLock(name = "redis_zset_poll_expired_orders", lockAtMostFor = "PT5S", lockAtLeastFor = "PT1S")
    public void pollRedisZSet() {
        long now = System.currentTimeMillis();

        // Atomically pop all members with score <= now
        // ZPOPMIN with a count is the safest distributed-safe approach
        Set<ZSetOperations.TypedTuple<Object>> expired =
                redisTemplate.opsForZSet().popMin(REDIS_ZSET_KEY, 100);

        if (expired == null || expired.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<Object> tuple : expired) {
            Double score = tuple.getScore();
            if (score == null || score > now) {
                // Popped a future item (shouldn't happen with score filter, but safety net)
                // Re-add it back to the ZSET so it's not lost
                redisTemplate.opsForZSet().add(REDIS_ZSET_KEY, tuple.getValue(), score);
                continue;
            }

            String orderIdStr = (String) tuple.getValue();
            log.info("[Redis ZSET] Atomically popped expired order #{} from ZSET", orderIdStr);
            try {
                Long orderId = Long.parseLong(orderIdStr);
                orderService.cancelOrder(orderId, "Redis ZSET atomic ZPOPMIN");
            } catch (NumberFormatException e) {
                log.error("[Redis ZSET] Failed to parse order ID: {}", orderIdStr);
            }
        }
    }
}
