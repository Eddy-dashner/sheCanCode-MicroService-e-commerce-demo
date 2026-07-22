package com.shop.productservice.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** The authoritative current price. This is what order-service will ask for later. */
public record PriceResponse(UUID productId, BigDecimal price) {
}
