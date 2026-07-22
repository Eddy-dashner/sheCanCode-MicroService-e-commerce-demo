package com.shop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Emitted by payment-service when a charge is authorised. Moves the order to CONFIRMED. */
public record PaymentAuthorizedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID paymentId,
        BigDecimal amount
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
