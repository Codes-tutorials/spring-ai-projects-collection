package com.example.ordercancel.dto;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Order response DTO — decoupled from the database entity")
public class OrderResponseDto {

    @Schema(description = "Unique order ID", example = "42")
    private Long id;

    @Schema(description = "Current order status")
    private OrderStatus status;

    @Schema(description = "Cancellation strategy used for this order")
    private CancellationApproach approach;

    @Schema(description = "Order total in USD", example = "149.99")
    private BigDecimal amount;

    @Schema(description = "Timestamp when the order was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the order expires if unpaid")
    private LocalDateTime expireAt;
}
