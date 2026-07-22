package com.shop.paymentservice.service;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID orderId) {
        super("No payment for order: " + orderId);
    }
}
