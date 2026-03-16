package com.messaging.apigateway.api.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthResponse(
        String token,
        String tokenType,

        UUID userId,
        String username,
        String displayName
) {
}
