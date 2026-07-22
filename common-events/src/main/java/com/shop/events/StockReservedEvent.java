package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/** Emitted by inventory-service when stock for an order was successfully reserved. */
public record StockReservedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID reservationId
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
