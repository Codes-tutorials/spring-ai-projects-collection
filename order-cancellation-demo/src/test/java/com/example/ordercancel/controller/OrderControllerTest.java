package com.example.ordercancel.controller;

import com.example.ordercancel.dto.CreateOrderRequest;
import com.example.ordercancel.dto.OrderResponseDto;
import com.example.ordercancel.exception.OrderNotFoundException;
import com.example.ordercancel.mapper.OrderMapper;
import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController API Tests")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OrderService orderService;
    @MockBean private OrderMapper orderMapper;

    private OrderResponseDto sampleDto() {
        return OrderResponseDto.builder()
                .id(1L)
                .status(OrderStatus.UNPAID)
                .approach(CancellationApproach.DELAY_QUEUE)
                .amount(new BigDecimal("149.99"))
                .createdAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusSeconds(30))
                .build();
    }

    private Order sampleOrder() {
        return Order.builder()
                .id(1L)
                .status(OrderStatus.UNPAID)
                .approach(CancellationApproach.DELAY_QUEUE)
                .amount(new BigDecimal("149.99"))
                .createdAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusSeconds(30))
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders: should return 201 with valid request")
    void createOrder_validRequest_returns201() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(149.99);
        req.setApproach(CancellationApproach.DELAY_QUEUE);
        req.setDelaySeconds(30);

        when(orderService.createOrder(any(), any(), anyInt())).thenReturn(sampleOrder());
        when(orderMapper.toDto(any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("UNPAID"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders: should return 400 when amount is missing")
    void createOrder_missingAmount_returns400() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setApproach(CancellationApproach.DELAY_QUEUE);
        // amount intentionally null

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders: should return 400 when amount is negative")
    void createOrder_negativeAmount_returns400() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(-10.0);
        req.setApproach(CancellationApproach.DELAY_QUEUE);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders/{id}: should return 404 when order not found")
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").contains("99"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders: should return paginated results")
    void getAllOrders_returnsPaginatedResults() throws Exception {
        Page<Order> page = new PageImpl<>(List.of(sampleOrder()));
        when(orderService.getAllOrders(any())).thenReturn(page);
        when(orderMapper.toDto(any(Order.class))).thenReturn(sampleDto());

        mockMvc.perform(get("/api/orders?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
