package com.example.ordercancel;

import com.example.ordercancel.dto.CreateOrderRequest;
import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test — Order Auto-Cancellation
 * -------------------------------------------
 * Uses the 'test' profile with H2 in-memory database.
 * Tests the DelayQueue and DB Polling strategies end-to-end without external dependencies.
 *
 * For full Testcontainers integration (PostgreSQL + RabbitMQ + Redis),
 * see the companion test with @Testcontainers annotations (requires Docker).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Order Cancellation Integration Tests (H2)")
class OrderCancellationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;

    @Test
    @WithMockUser
    @DisplayName("DelayQueue: order should be CANCELLED after delay expires")
    void delayQueue_orderCancelledAfterDelay() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(100.0);
        req.setApproach(CancellationApproach.DELAY_QUEUE);
        req.setDelaySeconds(3); // 3 seconds for fast test

        String responseBody = mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(responseBody).get("id").asLong();

        // Wait up to 6 seconds for the order to be CANCELLED
        await().atMost(6, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(orderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
    }

    @Test
    @WithMockUser
    @DisplayName("DelayQueue: order should remain PAID when paid before expiry")
    void delayQueue_orderRemainsPayedWhenPaidBeforeExpiry() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(200.0);
        req.setApproach(CancellationApproach.DELAY_QUEUE);
        req.setDelaySeconds(5);

        String responseBody = mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(responseBody).get("id").asLong();

        // Pay immediately before the 5s timer fires
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/" + orderId + "/pay")
                        .with(csrf()))
                .andExpect(status().isOk());

        // After 6s, verify still PAID (not cancelled)
        await().atMost(7, TimeUnit.SECONDS)
                .pollDelay(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(orderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
                });
    }

    @Test
    @WithMockUser
    @DisplayName("DB Polling: order should be CANCELLED within 5s poll interval after expiry")
    void dbPolling_orderCancelledWithinPollInterval() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAmount(75.0);
        req.setApproach(CancellationApproach.DB_POLLING);
        req.setDelaySeconds(2); // 2s expiry + max 5s poll = 7s max total

        String responseBody = mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(responseBody).get("id").asLong();

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(orderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
    }
}
