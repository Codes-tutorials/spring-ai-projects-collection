package com.example.ordercancel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_status_expire_approach",
               columnList = "status, expire_at, approach")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancellationApproach approach;

    /**
     * Use BigDecimal for monetary values — never use Double/Float for money.
     * Avoids IEEE 754 floating-point precision errors.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    /**
     * Optimistic locking — prevents two concurrent threads from both reading
     * UNPAID and both writing CANCELLED. The second write will throw
     * ObjectOptimisticLockingFailureException and be retried.
     */
    @Version
    private Long version;
}
