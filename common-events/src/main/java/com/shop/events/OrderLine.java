package com.shop.events;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID productId, int quantity, BigDecimal unitPrice) {
}
