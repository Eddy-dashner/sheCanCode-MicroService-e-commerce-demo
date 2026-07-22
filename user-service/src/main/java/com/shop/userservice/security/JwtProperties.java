package com.shop.userservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised JWT settings. The secret is injected from the environment via
 * config-server (never hard-coded here), keeping secrets out of the repo.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long expirationMinutes, String issuer) {
}
