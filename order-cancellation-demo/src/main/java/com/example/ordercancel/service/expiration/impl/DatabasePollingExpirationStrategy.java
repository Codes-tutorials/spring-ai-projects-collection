package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.repository.OrderRepository;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DatabasePollingExpirationStrategy implements OrderExpirationStrategy {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Autowired
    public DatabasePollingExpirationStrategy(OrderRepository orderRepository, @Lazy OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.DB_POLLING;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        log.info("[DB Polling] Order #{} registered. Scheduler will pick it up within 5s of expiry.", order.getId());
    }

    /**
     * Poll the database every 5 seconds for expired UNPAID orders.
     *
     * @SchedulerLock: ensures only ONE node in a cluster executes this at a time.
     * lockAtMostFor = how long the lock is held even if the node dies.
     * lockAtLeastFor = prevents back-to-back executions from the same node.
     */
    @Scheduled(fixedRate = 5000)
    @SchedulerLock(name = "db_poll_expired_orders", lockAtMostFor = "PT10S", lockAtLeastFor = "PT4S")
    public void pollDatabase() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndApproachAndExpireAtBefore(
                OrderStatus.UNPAID,
                CancellationApproach.DB_POLLING,
                now
        );

        if (!expiredOrders.isEmpty()) {
            log.info("[DB Polling] Scheduler found {} expired unpaid orders to cancel.", expiredOrders.size());
            for (Order order : expiredOrders) {
                orderService.cancelOrder(order.getId(), "Database Polling scheduler");
            }
        }
    }
}
