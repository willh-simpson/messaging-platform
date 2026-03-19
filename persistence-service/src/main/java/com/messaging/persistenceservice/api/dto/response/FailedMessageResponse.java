package com.messaging.persistenceservice.api.dto.response;

import com.messaging.persistenceservice.domain.model.FailedMessage;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FailedMessageResponse(
        UUID id,

        UUID messageId,
        UUID channelId,
        UUID authorId,
        String authorUsername,
        String content,
        Instant originalCreatedAt,

        String sourceTopic,
        String correlationId,

        Instant failedAt,

        FailedMessage.FailedMessageStatus status,

        Instant acknowledgedAt,
        String resolutionNote
) {
    public static FailedMessageResponse toResponse(FailedMessage message) {
        return FailedMessageResponse.builder()
                .id(message.getId())
                .messageId(message.getMessageId())
                .channelId(message.getChannelId())
                .authorId(message.getAuthorId())
                .authorUsername(message.getAuthorUsername())
                .content(message.getContent())
                .originalCreatedAt(message.getOriginalCreatedAt())
                .sourceTopic(message.getSourceTopic())
                .correlationId(message.getCorrelationId())
                .failedAt(message.getFailedAt())
                .status(message.getStatus())
                .acknowledgedAt(message.getAcknowledgedAt())
                .resolutionNote(message.getResolutionNote())
                .build();
    }
}
