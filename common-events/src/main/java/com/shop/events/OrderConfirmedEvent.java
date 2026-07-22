package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by order-service when an order reaches its terminal success state.
 * inventory-service consumes it to finalise (confirm) the stock reservation.
 */
public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
