package com.messaging.persistenceservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

/**
 * MongoDB document recording messages exhausted by Kafka retries and landed in Dead Letter Topic.
 */
@Document("failed_messages")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedMessage {
    @Id
    private UUID id;

    @Field("message_id")
    private UUID messageId;
    @Field("channel_id")
    private UUID channelId;
    @Field("author_id")
    private UUID authorId;
    @Field("author_username")
    private String authorUsername;
    private String content;
    @Field("original_created_at")
    private Instant originalCreatedAt;

    @Field("source_topic")
    private String sourceTopic;
    @Field("correlation_id")
    private String correlationId;

    @Indexed
    @Field("failed_at")
    private Instant failedAt;

    @Indexed
    @Builder.Default
    private FailedMessageStatus status = FailedMessageStatus.PENDING;

    @Field("acknowledged_at")
    private Instant acknowledgedAt;
    @Field("resolution_note")
    private String resolutionNote;

    public enum FailedMessageStatus {
        PENDING, ACKNOWLEDGED, RESOLVED
    }
}
