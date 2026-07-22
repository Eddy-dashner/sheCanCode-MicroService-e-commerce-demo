package com.shop.inventoryservice.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.Map;
import java.util.UUID;

/**
 * A hold placed against stock for one order. Records exactly what was reserved so
 * it can be released later (the compensating step) with the correct quantities.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    public enum Status {ACTIVE, RELEASED, CONFIRMED}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // productId -> quantity reserved for this order.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_lines", joinColumns = @JoinColumn(name = "reservation_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<UUID, Integer> lines;

    protected Reservation() { // JPA
    }

    public Reservation(UUID orderId, Map<UUID, Integer> lines) {
        this.orderId = orderId;
        this.lines = lines;
        this.status = Status.ACTIVE;
    }

    public void markReleased() {
        this.status = Status.RELEASED;
    }

    public void markConfirmed() {
        this.status = Status.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Status getStatus() {
        return status;
    }

    public Map<UUID, Integer> getLines() {
        return lines;
    }
}
