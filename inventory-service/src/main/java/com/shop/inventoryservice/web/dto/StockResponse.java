package com.shop.inventoryservice.web.dto;

import com.shop.inventoryservice.domain.StockItem;

import java.util.UUID;

public record StockResponse(UUID productId, int available, int reserved, int lowStockThreshold) {
    public static StockResponse from(StockItem item) {
        return new StockResponse(item.getProductId(), item.getAvailableQuantity(),
                item.getReservedQuantity(), item.getLowStockThreshold());
    }
}
