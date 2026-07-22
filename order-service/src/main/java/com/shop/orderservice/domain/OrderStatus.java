package com.shop.orderservice.domain;

import java.util.Set;

/**
 * The order state machine. Encodes which transitions are legal so an out-of-order
 * or duplicate event can't drive an order into a nonsensical state.
 *
 *   CREATED --StockReserved--> STOCK_RESERVED --> PAYMENT_PENDING
 *      \--StockReservationFailed--> CANCELLED           │
 *                                          PaymentAuthorized --> CONFIRMED
 *                                          PaymentFailed     --> CANCELLED
 *
 * PAYMENT_PENDING was slotted in when payment-service arrived (Phase 6): the
 * order now waits for a payment outcome instead of confirming straight after
 * stock. This enum + OrderService were essentially the ONLY things that changed.
 */
public enum OrderStatus {
    CREATED,
    STOCK_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    CANCELLED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED = java.util.Map.of(
            CREATED, Set.of(STOCK_RESERVED, CANCELLED),
            STOCK_RESERVED, Set.of(PAYMENT_PENDING, CANCELLED),
            PAYMENT_PENDING, Set.of(CONFIRMED, CANCELLED),
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
