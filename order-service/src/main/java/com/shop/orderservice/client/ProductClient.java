package com.shop.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * The canonical synchronous call of the whole system: at order-placement time we
 * MUST know the authoritative current price, so we ask product-service directly
 * (product owns pricing). Timeout/retry/circuit-breaker config lives in
 * order-service.yml; the fallback is ProductClientFallback.
 */
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{id}/price")
    ProductPrice getPrice(@PathVariable("id") UUID id);
}
