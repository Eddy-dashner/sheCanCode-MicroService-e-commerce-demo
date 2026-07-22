package com.shop.orderservice.domain;

import java.util.UUID;

public class IllegalOrderTransitionException extends RuntimeException {
    public IllegalOrderTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot transition from " + from + " to " + to);
    }
}
