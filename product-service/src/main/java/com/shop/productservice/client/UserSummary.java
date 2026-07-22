package com.shop.productservice.client;

import java.util.UUID;

/**
 * product-service's OWN view of a user — only the fields it needs. It does NOT
 * import user-service's classes: sharing entity/DTO classes across services would
 * couple their deploys together. Each service models its neighbours locally.
 */
public record UserSummary(UUID id, String email, String firstName, String lastName) {

    public static UserSummary unknown(UUID id) {
        return new UserSummary(id, "unknown", "unknown", "unknown");
    }
}
