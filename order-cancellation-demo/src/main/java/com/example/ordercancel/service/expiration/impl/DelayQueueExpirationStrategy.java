package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DelayQueueExpirationStrategy implements OrderExpirationStrategy {

    private final OrderService orderService;
    private final DelayQueue<DelayedOrder> delayQueue = new DelayQueue<>();

    @Autowired
    public DelayQueueExpirationStrategy(@Lazy OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.DELAY_QUEUE;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        log.info("[DelayQueue] Scheduling expiration for order #{} in {}s", order.getId(), delaySeconds);
        delayQueue.put(new DelayedOrder(order.getId(), delaySeconds));
    }

    @PostConstruct
    public void startConsumer() {
        Thread thread = new Thread(() -> {
            log.info("[DelayQueue] Daemon consumer thread started.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                     DelayedOrder expiredOrder = delayQueue.take();
                     log.info("[DelayQueue] Order #{} delay expired, executing cancellation check.", expiredOrder.getOrderId());
                     orderService.cancelOrder(expiredOrder.getOrderId(), "Java DelayQueue consumer thread");
                } catch (InterruptedException e) {
                     log.info("[DelayQueue] Consumer thread interrupted. Stopping.");
                     Thread.currentThread().interrupt();
                     break;
                } catch (Exception e) {
                     log.error("[DelayQueue] Error during delay processing", e);
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("java-delayqueue-consumer");
        thread.start();
    }

    // Helper static class to represent delayed orders
    private static class DelayedOrder implements Delayed {
        private final Long orderId;
        private final long triggerTimeNs;

        public DelayedOrder(Long orderId, long delaySeconds) {
            this.orderId = orderId;
            this.triggerTimeNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(delaySeconds);
        }

        public Long getOrderId() {
            return orderId;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diffNs = triggerTimeNs - System.nanoTime();
            return unit.convert(diffNs, TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (this.triggerTimeNs < ((DelayedOrder) other).triggerTimeNs) {
                return -1;
            }
            if (this.triggerTimeNs > ((DelayedOrder) other).triggerTimeNs) {
                return 1;
            }
            return 0;
        }
    }
}
