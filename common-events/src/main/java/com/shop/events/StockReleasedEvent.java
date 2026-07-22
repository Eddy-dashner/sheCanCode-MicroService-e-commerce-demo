package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * The COMPENSATING event: emitted when a previously-made reservation is released
 * (e.g. payment later failed and the order was cancelled). This is how a saga
 * "undoes" a step without a distributed transaction/rollback.
 */
public record StockReleasedEvent(
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
