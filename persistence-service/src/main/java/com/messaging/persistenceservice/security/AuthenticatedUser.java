package com.messaging.persistenceservice.security;

import java.util.UUID;

/**
 * The principal stored in SecurityContext.
 * This pulls user ID directly from SecurityContextHolder without parsing token again.
 * <p>
 * This is used to avoid querying API Gateway Service.
 * </p>
 */
public record AuthenticatedUser(
        UUID userId,
        String username
) {
}
