package com.example.ordercancel.service;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrderService {
    Order createOrder(BigDecimal amount, CancellationApproach approach, int delaySeconds);
    Order payOrder(Long id);
    void cancelOrder(Long id, String reason);
    Page<Order> getAllOrders(Pageable pageable);
    Order getOrderById(Long id);
}
