package com.shop.productservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The graceful degradation path. When user-service is down/slow and the circuit
 * opens, calls are routed here INSTANTLY instead of piling up waiting for a dead
 * service. We return a placeholder so the catalogue still works — the product's
 * creator just shows as "unknown". Failing soft beats failing hard.
 */
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public UserSummary getUser(UUID id) {
        log.warn("user-service unavailable (circuit open or call failed); using fallback for user {}", id);
        return UserSummary.unknown(id);
    }
}
