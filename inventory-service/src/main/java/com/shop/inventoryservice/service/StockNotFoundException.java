package com.shop.inventoryservice.service;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(UUID productId) {
        super("No stock record for product: " + productId);
    }
}
