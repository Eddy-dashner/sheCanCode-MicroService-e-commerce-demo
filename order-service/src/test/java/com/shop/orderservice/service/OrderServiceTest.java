package com.shop.orderservice.service;

import com.shop.events.OrderConfirmedEvent;
import com.shop.events.OrderCreatedEvent;
import com.shop.events.OrderCancelledEvent;
import com.shop.events.StockReservationFailedEvent;
import com.shop.events.StockReservedEvent;
import com.shop.orderservice.client.ProductClient;
import com.shop.orderservice.client.ProductPrice;
import com.shop.orderservice.domain.Order;
import com.shop.orderservice.domain.OrderStatus;
import com.shop.orderservice.messaging.OrderEventPublisher;
import com.shop.orderservice.repository.OrderRepository;
import com.shop.orderservice.repository.ProcessedEventRepository;
import com.shop.orderservice.web.dto.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock ProcessedEventRepository processedEvents;
    @Mock ProductClient productClient;
    @Mock OrderEventPublisher publisher;
    @InjectMocks OrderService orderService;

    @Test
    void place_fetchesAuthoritativePrice_savesCreated_andPublishesOrderCreated() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productClient.getPrice(productId)).thenReturn(new ProductPrice(productId, new BigDecimal("10.00")));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new PlaceOrderRequest(List.of(new PlaceOrderRequest.Line(productId, 3)));
        Order order = orderService.place(userId, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("30.00");
        verify(publisher).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void stockReserved_confirmsOrder_andPublishesConfirmed() {
        Order order = new Order(UUID.randomUUID(),
                List.of(new com.shop.orderservice.domain.OrderItem(UUID.randomUUID(), 1, new BigDecimal("5.00"))));
        UUID orderId = order.getId();
        when(processedEvents.existsById(any())).thenReturn(false);
        when(orders.findById(orderId)).thenReturn(Optional.of(order));

        orderService.onStockReserved(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), orderId, UUID.randomUUID()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(publisher).publish(any(OrderConfirmedEvent.class));
    }

    @Test
    void stockReservationFailed_cancelsOrder_andPublishesCancelled() {
        Order order = new Order(UUID.randomUUID(),
                List.of(new com.shop.orderservice.domain.OrderItem(UUID.randomUUID(), 1, new BigDecimal("5.00"))));
        UUID orderId = order.getId();
        when(processedEvents.existsById(any())).thenReturn(false);
        when(orders.findById(orderId)).thenReturn(Optional.of(order));

        orderService.onStockReservationFailed(new StockReservationFailedEvent(
                UUID.randomUUID(), Instant.now(), orderId, "out of stock", List.of()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).contains("out of stock");
        verify(publisher).publish(any(OrderCancelledEvent.class));
    }

    @Test
    void duplicateEvent_isIgnored() {
        when(processedEvents.existsById(any())).thenReturn(true);

        orderService.onStockReserved(new StockReservedEvent(
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID()));

        verify(orders, never()).findById(any());
        verify(publisher, never()).publish(any());
    }
}
