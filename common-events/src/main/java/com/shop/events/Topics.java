package com.shop.events;

/**
 * The Kafka topic names, shared so producers and consumers can't drift on
 * spelling. These are plain constants (contract, not behaviour), which is why
 * they're allowed to live in common-events.
 */
public final class Topics {

    private Topics() {
    }

    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String PAYMENT_EVENTS = "payment-events";
}
