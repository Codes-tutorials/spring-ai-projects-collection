package com.example.ordercancel.repository;

import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find all UNPAID orders that have expired
    List<Order> findByStatusAndExpireAtBefore(OrderStatus status, LocalDateTime dateTime);
    
    // Find all UNPAID orders for a specific approach that have expired
    List<Order> findByStatusAndApproachAndExpireAtBefore(OrderStatus status, com.example.ordercancel.model.CancellationApproach approach, LocalDateTime dateTime);
}
