package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a product's available stock drops to/below its threshold. Not part
 * of the checkout saga — a notification other systems (e.g. procurement) may act
 * on. Its correlation id is the productId.
 */
public record LowStockEvent(
        UUID eventId,
        Instant occurredAt,
        UUID productId,
        int availableQuantity,
        int threshold
) implements DomainEvent {

    @Override
    public UUID correlationId() {
        return productId;
    }
}
