package com.messaging.persistenceservice.api.dto;

import com.messaging.persistenceservice.domain.model.Message;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MessageResponse(
        UUID messageId,

        UUID channelId,
        UUID authorId,
        String authorUsername,
        String content,

        Instant createdAt
) {
    public static MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .messageId(message.getId())
                .channelId(message.getChannelId())
                .authorId(message.getAuthorId())
                .authorUsername(message.getAuthorUsername())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
