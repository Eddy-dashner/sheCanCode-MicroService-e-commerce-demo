package com.shop.orderservice.domain;

import java.util.Set;

/**
 * The order state machine. Encodes which transitions are legal so an out-of-order
 * or duplicate event can't drive an order into a nonsensical state.
 *
 *   CREATED --StockReserved--> STOCK_RESERVED --(no payment yet)--> CONFIRMED
 *      \--StockReservationFailed--> CANCELLED
 *   STOCK_RESERVED --cancel--> CANCELLED
 *
 * When payment-service arrives (Phase 6), PAYMENT_PENDING/PAYMENT_* slot in
 * between STOCK_RESERVED and CONFIRMED — and this enum is the main thing that changes.
 */
public enum OrderStatus {
    CREATED,
    STOCK_RESERVED,
    CONFIRMED,
    CANCELLED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED = java.util.Map.of(
            CREATED, Set.of(STOCK_RESERVED, CANCELLED),
            STOCK_RESERVED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED;
    }
}
