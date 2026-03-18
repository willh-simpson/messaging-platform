package com.messaging.deliveryservice.api.dto;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON object pushed to WebSocket subscribers.
 */
@Builder
public record MessageDeliveryPayload(
        UUID messageId,

        UUID channelId,
        UUID authorID,
        String authorUsername,
        String content,

        Instant createdAt
) {
    public static MessageDeliveryPayload wrapPayload(MessageCreatedEvent event) {
        return MessageDeliveryPayload.builder()
                .messageId(event.getMessageId())
                .channelId(event.getChannelId())
                .authorID(event.getAuthorId())
                .authorUsername(event.getAuthorUsername())
                .content(event.getContent())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
