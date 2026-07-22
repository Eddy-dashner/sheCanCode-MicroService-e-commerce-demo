package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by order-service when an order is cancelled (e.g. payment failed).
 * inventory-service consumes this to release any reservation it holds.
 */
public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String reason
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
