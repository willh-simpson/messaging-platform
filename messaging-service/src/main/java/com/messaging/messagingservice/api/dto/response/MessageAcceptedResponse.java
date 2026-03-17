package com.messaging.messagingservice.api.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MessageAcceptedResponse(
        UUID messageId,
        UUID channelId,
        Instant acceptedAt,
        String status
) {
}
