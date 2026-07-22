package com.shop.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Emitted by inventory-service when stock could not be reserved (out of stock). */
public record StockReservationFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String reason,
        List<UUID> unavailableProductIds
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return orderId;
    }
}
