package com.shop.orderservice.client;

import java.util.UUID;

public class PriceUnavailableException extends RuntimeException {
    public PriceUnavailableException(UUID productId) {
        super("Could not obtain an authoritative price for product " + productId
                + "; order rejected.");
    }
}
