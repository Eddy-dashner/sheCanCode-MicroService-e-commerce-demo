package com.shop.inventoryservice.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetStockRequest(
        @NotNull UUID productId,
        @Min(0) int quantity,
        @Min(0) int lowStockThreshold
) {
}
