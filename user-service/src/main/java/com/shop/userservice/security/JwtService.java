package com.shop.userservice.security;

import com.shop.userservice.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Issues signed JWTs. user-service is the ONLY issuer; the gateway (and later,
 * other services) only ever VALIDATE tokens with the same shared secret. Keeping
 * issuance in one place means one service owns identity.
 *
 * We use a symmetric HS256 key for simplicity. A production system would prefer
 * asymmetric RS256/ES256 so validators need only the public key and can never
 * mint tokens — call that out as a deliberate learning simplification.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = props.expirationMinutes();
        this.issuer = props.issuer();
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        List<String> roleNames = user.getRoles().stream().map(Enum::name).toList();

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roleNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
