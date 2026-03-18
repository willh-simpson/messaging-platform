package com.messaging.deliveryservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * JWT validation for Delivery Service.
 *
 * @apiNote Does not generate or provide tokens. That functionality is in API Gateway Service.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract user ID from valid token.
     *
     * @param token JWT token from user's session.
     * @return User ID.
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * Extracts username from valid token.
     *
     * @param token JWT token from user's session.
     * @return Username.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /**
     * Validate token's signature for callers.
     *
     * @param token JWT token from user's session.
     * @return Whether token is valid and user is authenticated.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);

            return true;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("JWT error: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Parse token and extract claims (payload).
     *
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
