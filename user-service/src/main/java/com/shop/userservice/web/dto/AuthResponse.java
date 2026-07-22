package com.shop.userservice.web.dto;

/** Returned on successful login. The client sends this token as a Bearer header. */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
    public static AuthResponse bearer(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
