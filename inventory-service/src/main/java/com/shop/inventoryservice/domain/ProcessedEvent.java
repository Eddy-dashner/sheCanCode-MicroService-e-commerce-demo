package com.shop.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The idempotency ledger. Every event we successfully handle has its eventId
 * recorded here. Before handling an event we check this table; if the id is
 * already present we skip it. That makes consumers safe against Kafka's
 * at-least-once delivery, where the same message can arrive more than once.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() { // JPA
    }

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
