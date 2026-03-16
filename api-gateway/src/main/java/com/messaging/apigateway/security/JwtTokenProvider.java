package com.messaging.apigateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and validates JSON Web Tokens (JWTs).
 */
@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generate JWT for unique user.
     *
     * @param userId User's unique UUID. Required for inter-service communication.
     * @param username Included for convenience by avoiding excess DB lookup.
     * @return JWT session token.
     */
    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract user ID from valid token.
     *
     * @param token JWT token from user's session.
     * @return User ID.
     */
    public UUID getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();

        return UUID.fromString(subject);
    }

    /**
     * Extracts username from valid token.
     * @param token JWT token from user's session.
     * @return Username.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /**
     * Validate token's signature and expiry for callers.
     *
     * @param token JWT token from user's session.
     * @return Whether token is valid and user is authenticated.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);

            return true;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT structure: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Empty JWT claims: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Parse token and extract claims (payload).
     * @param token JWT token from user's session.
     * @return Claims information on user id and username.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
