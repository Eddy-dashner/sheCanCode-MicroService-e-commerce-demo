package com.shop.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Edge authentication. This is the ONE place tokens are validated, so downstream
 * services never re-implement JWT parsing. On a valid token it forwards the
 * caller's identity as trusted headers (X-User-Id / X-User-Roles); it also
 * STRIPS any client-supplied copies of those headers so a caller can't spoof them.
 *
 * Public routes (registration, login, docs, health) skip validation.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    private final SecretKey key;

    public JwtAuthenticationFilter(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublic(request)) {
            // Even on public routes, drop any spoofed identity headers.
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(7))
                    .getPayload();

            String userId = claims.getSubject();
            List<?> roles = claims.get("roles", List.class);
            String rolesCsv = roles == null ? "" : String.join(",", roles.stream().map(Object::toString).toList());

            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> h.remove(USER_ID_HEADER))
                    .headers(h -> h.remove(ROLES_HEADER))
                    .header(USER_ID_HEADER, userId)
                    .header(ROLES_HEADER, rolesCsv)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        String method = request.getMethod().name();
        boolean register = "POST".equals(method) && path.equals("/users");
        // Catalogue reads are public; creating a product still needs a token.
        boolean publicCatalogueRead = "GET".equals(method) && path.startsWith("/products");
        return register
                || publicCatalogueRead
                || path.startsWith("/auth/")
                || path.startsWith("/wiring-check")
                || path.contains("/actuator")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs");
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> h.remove(USER_ID_HEADER))
                .headers(h -> h.remove(ROLES_HEADER))
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // Run before routing so identity is resolved before the request is proxied.
        return -1;
    }
}
