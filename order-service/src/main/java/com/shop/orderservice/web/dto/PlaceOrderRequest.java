package com.shop.orderservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotNull UUID productId, @Min(1) int quantity) {
    }
}
