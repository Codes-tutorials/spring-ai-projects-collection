package com.example.ordercancel.service.expiration.impl;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.OrderService;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class NettyWheelExpirationStrategy implements OrderExpirationStrategy, TimerTask {

    private final OrderService orderService;
    private final HashedWheelTimer timer;

    @Autowired
    public NettyWheelExpirationStrategy(@Lazy OrderService orderService, HashedWheelTimer timer) {
        this.orderService = orderService;
        this.timer = timer;
    }

    @Override
    public boolean supports(CancellationApproach approach) {
        return approach == CancellationApproach.TIME_WHEEL;
    }

    @Override
    public void scheduleExpiration(Order order, int delaySeconds) {
        log.info("[Time Wheel] Scheduling expiration for order #{} in {}s using HashedWheelTimer", order.getId(), delaySeconds);
        
        // Schedule new timeout on the hashed wheel timer
        timer.newTimeout(timeout -> {
            log.info("[Time Wheel] HashedWheelTimer ticked for order #{}", order.getId());
            orderService.cancelOrder(order.getId(), "Netty HashedWheelTimer");
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        // Not used as we define an inline lambda inside newTimeout
    }
}
