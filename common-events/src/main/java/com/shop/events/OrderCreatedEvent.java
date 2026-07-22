package com.shop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emitted by order-service when a customer places an order. Kicks off the saga.
 * inventory-service consumes this to try to reserve stock.
 */
public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        List<OrderLine> lines,
        BigDecimal totalAmount
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
