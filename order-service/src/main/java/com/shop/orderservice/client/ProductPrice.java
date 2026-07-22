package com.shop.orderservice.client;

import java.math.BigDecimal;
import java.util.UUID;

/** order-service's local view of the price reply. Not shared with product-service. */
public record ProductPrice(UUID productId, BigDecimal price) {
}
