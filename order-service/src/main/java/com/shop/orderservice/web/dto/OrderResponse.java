package com.shop.orderservice.web.dto;

import com.shop.orderservice.domain.Order;
import com.shop.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<Item> items,
        String cancelReason,
        Instant createdAt
) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(),
                order.getTotalAmount(), items, order.getCancelReason(), order.getCreatedAt());
    }
}
