package com.shop.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Common envelope every event carries.
 *
 * @return eventId a unique id per event instance — consumers use it to detect
 * and drop duplicates (idempotency).
 */
public interface DomainEvent {

    /** Unique id of THIS event occurrence; the key to idempotent consumption. */
    UUID eventId();

    /** When the event happened, in the producer's clock. */
    Instant occurredAt();

    /**
     * Correlation id that ties every event in one checkout saga together. It is
     * the orderId, so a whole saga can be traced end to end.
     */
    UUID correlationId();
}
