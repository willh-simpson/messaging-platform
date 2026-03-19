package com.messaging.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka when message is accepted.
 * All downstream services depend on this event.
 */
@Value
@Builder
@Jacksonized
public class MessageCreatedEvent {
        UUID messageId;
        UUID channelId;
        UUID authorId;

        String authorUsername;
        String content;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt;

        @Builder.Default
        String eventType = "MESSAGE_CREATED";

        String correlationId;
        @Builder.Default
        int schemaVersion = 1;
}
