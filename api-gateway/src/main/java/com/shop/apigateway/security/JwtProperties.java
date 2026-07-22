package com.shop.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Same secret user-service issues with; the gateway only validates. */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret) {
}
