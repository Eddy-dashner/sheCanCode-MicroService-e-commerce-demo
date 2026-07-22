package com.shop.orderservice.domain;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Order() { // JPA
    }

    public Order(UUID userId, List<OrderItem> items) {
        this.userId = userId;
        this.items = items;
        this.status = OrderStatus.CREATED;
        this.totalAmount = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * The ONE place a status changes. It rejects illegal transitions, which also
     * makes it naturally idempotent-friendly: a repeated event that would repeat a
     * transition is refused rather than silently re-applied.
     */
    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalOrderTransitionException(id, status, target);
        }
        this.status = target;
    }

    public void cancel(String reason) {
        transitionTo(OrderStatus.CANCELLED);
        this.cancelReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
