package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by payment-service when a charge is declined. Triggers cancellation,
 * which in turn compensates the stock reservation (StockReleased).
 */
public record PaymentFailedEvent(
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
