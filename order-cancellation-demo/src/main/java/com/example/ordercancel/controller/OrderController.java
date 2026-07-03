package com.example.ordercancel.controller;

import com.example.ordercancel.dto.CreateOrderRequest;
import com.example.ordercancel.dto.OrderResponseDto;
import com.example.ordercancel.mapper.OrderMapper;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management and delayed cancellation simulation API")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Value("${app.order.expiration-delay-seconds:30}")
    private int defaultDelaySeconds;

    @Operation(summary = "Create a new order",
               description = "Creates an UNPAID order and schedules automatic cancellation via the chosen strategy")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request — validation failed")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        int delay = request.getDelaySeconds() != null ? request.getDelaySeconds() : defaultDelaySeconds;
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        Order order = orderService.createOrder(amount, request.getApproach(), delay);
        return orderMapper.toDto(order);
    }

    @Operation(summary = "Mark an order as paid",
               description = "Transitions an UNPAID order to PAID status. The expiration task will then no-op when it fires.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order paid successfully"),
        @ApiResponse(responseCode = "400", description = "Order is not in UNPAID status"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{id}/pay")
    public OrderResponseDto payOrder(
            @Parameter(description = "Order ID to pay", example = "1")
            @PathVariable Long id) {
        Order order = orderService.payOrder(id);
        return orderMapper.toDto(order);
    }

    @Operation(summary = "Get all orders (paginated)",
               description = "Returns a page of orders. Use ?page=0&size=20&sort=createdAt,desc")
    @GetMapping
    public Page<OrderResponseDto> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt")
            @Parameter(hidden = true) Pageable pageable) {
        return orderService.getAllOrders(pageable).map(orderMapper::toDto);
    }

    @Operation(summary = "Get a single order by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(orderMapper.toDto(order));
    }
}
