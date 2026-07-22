package com.shop.orderservice.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Unlike the product->user "creator" call, there is NO sensible soft fallback for
 * a price: we must not guess or use a stale price to charge a customer. So the
 * fallback fails LOUD — the order is rejected (503) rather than placed at an
 * unknown price. Fail-soft vs fail-loud is a per-call design decision.
 */
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductPrice getPrice(UUID id) {
        throw new PriceUnavailableException(id);
    }
}
