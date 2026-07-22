package com.shop.productservice.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "price must be positive")
        BigDecimal price,
        UUID categoryId
) {
}
