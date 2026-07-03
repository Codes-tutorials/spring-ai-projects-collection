package com.example.ordercancel.dto;

import com.example.ordercancel.model.CancellationApproach;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Request body for creating a new order")
public class CreateOrderRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be a positive value")
    @DecimalMax(value = "999999.99", message = "amount exceeds the maximum allowed value")
    @Schema(description = "Order amount in USD", example = "149.99")
    private Double amount;

    @NotNull(message = "approach is required")
    @Schema(description = "Cancellation strategy to use for this order")
    private CancellationApproach approach;

    @Min(value = 5, message = "delaySeconds must be at least 5 seconds")
    @Max(value = 3600, message = "delaySeconds cannot exceed 3600 seconds (1 hour)")
    @Schema(description = "Override the default expiration delay in seconds (5–3600)", example = "30")
    private Integer delaySeconds;
}
