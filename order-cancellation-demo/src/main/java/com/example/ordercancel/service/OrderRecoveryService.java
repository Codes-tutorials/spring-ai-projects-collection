package com.example.ordercancel.service;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.repository.OrderRepository;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Order Recovery Service
 * ----------------------
 * Addresses the "Lost Tasks on Restart" problem for in-memory strategies
 * (Java DelayQueue and Netty HashedWheelTimer).
 *
 * When the JVM restarts, all pending in-memory tasks are lost.
 * On startup, this service queries the DB for all UNPAID orders that still
 * have time remaining, and re-schedules them into the appropriate strategy.
 *
 * DB_POLLING and REDIS_ZSET do NOT need recovery — they continuously scan
 * their stores and will naturally pick up the orders in the next poll cycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRecoveryService {

    private static final Set<CancellationApproach> IN_MEMORY_APPROACHES = Set.of(
            CancellationApproach.DELAY_QUEUE,
            CancellationApproach.TIME_WHEEL
    );

    private final OrderRepository orderRepository;
    private final List<OrderExpirationStrategy> strategies;

    @EventListener(ApplicationStartedEvent.class)
    public void recoverPendingOrders() {
        LocalDateTime now = LocalDateTime.now();

        List<Order> pendingOrders = orderRepository.findByStatusAndExpireAtBefore(
                OrderStatus.UNPAID,
                now.plusYears(10)  // All UNPAID orders with any expiry
        );

        List<Order> inMemoryPending = pendingOrders.stream()
                .filter(o -> IN_MEMORY_APPROACHES.contains(o.getApproach()))
                .filter(o -> o.getExpireAt().isAfter(now))  // Still has time remaining
                .toList();

        if (inMemoryPending.isEmpty()) {
            log.info("[Recovery] No pending in-memory tasks to recover.");
            return;
        }

        log.warn("[Recovery] Recovering {} pending in-memory delayed tasks after restart.", inMemoryPending.size());

        for (Order order : inMemoryPending) {
            long remainingSeconds = java.time.Duration.between(now, order.getExpireAt()).getSeconds();
            if (remainingSeconds <= 0) {
                log.warn("[Recovery] Order #{} expiry is in the past ({}s ago). Scheduling immediate cancellation.",
                        order.getId(), -remainingSeconds);
                remainingSeconds = 1; // Schedule for immediate execution
            }

            strategies.stream()
                    .filter(s -> s.supports(order.getApproach()))
                    .findFirst()
                    .ifPresent(s -> {
                        log.info("[Recovery] Re-scheduling order #{} via {} with {}s remaining.",
                                order.getId(), order.getApproach(), remainingSeconds);
                        s.scheduleExpiration(order, (int) remainingSeconds);
                    });
        }
    }
}
