package com.example.ordercancel.service.impl;

import com.example.ordercancel.config.MetricsConfig;
import com.example.ordercancel.event.OrderEvent;
import com.example.ordercancel.exception.OrderNotFoundException;
import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.repository.OrderRepository;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final List<OrderExpirationStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;

    @Override
    @Transactional
    public Order createOrder(BigDecimal amount, CancellationApproach approach, int delaySeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusSeconds(delaySeconds);

        Order order = Order.builder()
                .amount(amount)
                .status(OrderStatus.UNPAID)
                .approach(approach)
                .createdAt(now)
                .expireAt(expireAt)
                .build();

        order = orderRepository.save(order);

        MDC.put("orderId", order.getId().toString());
        MDC.put("approach", approach.name());
        log.info("Created order #{} with approach {} expiring at {}", order.getId(), approach, expireAt);

        metricsConfig.incrementOrderCreated(approach.name());

        eventPublisher.publishEvent(new OrderEvent(this, order, "CREATED",
                String.format("Order #%d created. Expiration task scheduled in %ds via %s.",
                        order.getId(), delaySeconds, approach.name())));

        OrderExpirationStrategy strategy = strategies.stream()
                .filter(s -> s.supports(approach))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported approach: " + approach));

        strategy.scheduleExpiration(order, delaySeconds);

        MDC.remove("orderId");
        MDC.remove("approach");
        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        MDC.put("orderId", id.toString());

        if (order.getStatus() == OrderStatus.UNPAID) {
            order.setStatus(OrderStatus.PAID);
            order = orderRepository.save(order);
            log.info("Order #{} has been PAID", id);
            metricsConfig.incrementOrderPaid(order.getApproach().name());

            eventPublisher.publishEvent(new OrderEvent(this, order, "PAID",
                    String.format("Order #%d has been PAID. Expiration task will bypass cancellation.", id)));
        } else {
            log.warn("Attempted to pay order #{} but status is already {}", id, order.getStatus());
            throw new IllegalArgumentException(
                    String.format("Cannot pay order #%d — current status is %s", id, order.getStatus()));
        }

        MDC.remove("orderId");
        return order;
    }

    /**
     * Cancel an unpaid order.
     *
     * @Retryable: Retries up to 3 times on DataAccessException (transient DB errors).
     * This protects against a temporary DB outage causing a silently-lost cancellation.
     *
     * Idempotent: If the order is already PAID or CANCELLED, this is a no-op.
     */
    @Override
    @Transactional
    @Retryable(
        retryFor = DataAccessException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void cancelOrder(Long id, String reason) {
        MDC.put("orderId", id.toString());

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("Cancellation task fired for non-existent order #{}. Skipping.", id);
            MDC.remove("orderId");
            return;
        }

        if (order.getStatus() == OrderStatus.UNPAID) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order #{} CANCELLED. Reason: {}", id, reason);
            metricsConfig.incrementOrderCancelled(order.getApproach().name(), reason);

            eventPublisher.publishEvent(new OrderEvent(this, order, "CANCELLED",
                    String.format("Order #%d has been CANCELLED. Reason: %s", id, reason)));
        } else {
            // Idempotency: already paid or cancelled — do nothing
            log.debug("Cancellation task fired for order #{} but status is already {}. No-op.", id, order.getStatus());
            eventPublisher.publishEvent(new OrderEvent(this, order, "EXPIRED_CHECK",
                    String.format("Expiration task fired for Order #%d. Status is already %s. (No action taken)",
                            id, order.getStatus())));
        }

        MDC.remove("orderId");
    }

    /**
     * Recovery method — called when all @Retryable attempts are exhausted.
     * In production: send to a dead-letter topic, page on-call, or write to an audit log.
     */
    @Recover
    public void recoverCancelOrder(DataAccessException ex, Long id, String reason) {
        log.error("CRITICAL: Failed to cancel order #{} after all retry attempts. Reason was: {}. Error: {}",
                id, reason, ex.getMessage(), ex);
        // TODO: publish to alerting channel (PagerDuty, Slack, etc.)
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
