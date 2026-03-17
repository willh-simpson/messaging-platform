package com.messaging.messagingservice.security;

import java.util.UUID;

/**
 * The principal stored in SecurityContext.
 * This pulls user ID directly from SecurityContextHolder without parsing token again.
 * <p>
 * This is used to avoid linking to database in API Gateway Service, which would couple these 2 services.
 * </p>
 */
public record AuthenticatedUser(
        UUID userId,
        String username
) {
}
