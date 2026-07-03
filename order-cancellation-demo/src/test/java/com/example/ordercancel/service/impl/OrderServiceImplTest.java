package com.example.ordercancel.service.impl;

import com.example.ordercancel.config.MetricsConfig;
import com.example.ordercancel.event.OrderEvent;
import com.example.ordercancel.exception.OrderNotFoundException;
import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;
import com.example.ordercancel.model.OrderStatus;
import com.example.ordercancel.repository.OrderRepository;
import com.example.ordercancel.service.expiration.OrderExpirationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MetricsConfig metricsConfig;
    @Mock private OrderExpirationStrategy delayQueueStrategy;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleUnpaidOrder;

    @BeforeEach
    void setUp() {
        sampleUnpaidOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.UNPAID)
                .approach(CancellationApproach.DELAY_QUEUE)
                .amount(new BigDecimal("149.99"))
                .createdAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusSeconds(30))
                .version(0L)
                .build();

        // Configure the mock strategy to support DELAY_QUEUE
        when(delayQueueStrategy.supports(CancellationApproach.DELAY_QUEUE)).thenReturn(true);
        
        // Inject the strategy list into the service
        org.springframework.test.util.ReflectionTestUtils.setField(
                orderService, "strategies", List.of(delayQueueStrategy));
    }

    // ===== createOrder tests =====

    @Test
    @DisplayName("createOrder: should save order, emit event, and delegate to strategy")
    void createOrder_success() {
        when(orderRepository.save(any(Order.class))).thenReturn(sampleUnpaidOrder);

        Order result = orderService.createOrder(
                new BigDecimal("149.99"), CancellationApproach.DELAY_QUEUE, 30);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.UNPAID);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderEvent.class));
        verify(delayQueueStrategy, times(1)).scheduleExpiration(any(Order.class), eq(30));
        verify(metricsConfig, times(1)).incrementOrderCreated(CancellationApproach.DELAY_QUEUE.name());
    }

    @Test
    @DisplayName("createOrder: should throw when no strategy supports the approach")
    void createOrder_unsupportedApproach_throws() {
        when(delayQueueStrategy.supports(any())).thenReturn(false);

        assertThatThrownBy(() ->
                orderService.createOrder(BigDecimal.TEN, CancellationApproach.DB_POLLING, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported approach");
    }

    // ===== payOrder tests =====

    @Test
    @DisplayName("payOrder: should transition UNPAID -> PAID and emit event")
    void payOrder_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleUnpaidOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleUnpaidOrder);

        Order result = orderService.payOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(metricsConfig, times(1)).incrementOrderPaid(anyString());

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("payOrder: should throw OrderNotFoundException when order does not exist")
    void payOrder_notFound_throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.payOrder(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("payOrder: should throw IllegalArgumentException when order is already PAID")
    void payOrder_alreadyPaid_throws() {
        sampleUnpaidOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleUnpaidOrder));

        assertThatThrownBy(() -> orderService.payOrder(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAID");
    }

    // ===== cancelOrder tests =====

    @Test
    @DisplayName("cancelOrder: should transition UNPAID -> CANCELLED and emit event")
    void cancelOrder_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleUnpaidOrder));
        when(orderRepository.save(any())).thenReturn(sampleUnpaidOrder);

        orderService.cancelOrder(1L, "test reason");

        assertThat(sampleUnpaidOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(metricsConfig, times(1)).incrementOrderCancelled(anyString(), eq("test reason"));

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder: should be a no-op when order is already PAID (idempotency)")
    void cancelOrder_alreadyPaid_noOp() {
        sampleUnpaidOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleUnpaidOrder));

        orderService.cancelOrder(1L, "delayed task fired");

        verify(orderRepository, never()).save(any());
        verify(metricsConfig, never()).incrementOrderCancelled(anyString(), anyString());
    }

    @Test
    @DisplayName("cancelOrder: should silently skip when order ID doesn't exist")
    void cancelOrder_notFound_silentlySkips() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> orderService.cancelOrder(999L, "timer fired"));
        verify(orderRepository, never()).save(any());
    }
}
