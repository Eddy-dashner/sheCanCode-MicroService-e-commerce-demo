package com.shop.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Synchronous, discovery-resolved call to user-service. 'name' is the Eureka
 * service id, so Feign load-balances across its instances — we never hard-code a
 * host. 'fallback' names the bean used when the circuit is open or the call fails.
 *
 * Every sync call in this system MUST have a timeout, retry, circuit breaker and
 * fallback (see product-service.yml + this fallback). This is the reference example.
 */
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/users/{id}")
    UserSummary getUser(@PathVariable("id") UUID id);
}
