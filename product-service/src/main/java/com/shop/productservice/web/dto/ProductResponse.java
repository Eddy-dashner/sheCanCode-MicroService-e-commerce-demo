package com.shop.productservice.web.dto;

import com.shop.productservice.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        UUID categoryId,
        UUID createdBy
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCategoryId(), p.getCreatedBy());
    }
}
