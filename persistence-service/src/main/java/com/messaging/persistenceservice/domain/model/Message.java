package com.messaging.persistenceservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

/**
 * The MongoDB document written by persistence Kafka consumer.
 * Stores message history.
 * <p>
 * Messages are queried by last N messages in channel X, sorted by time, for page P.
 * Compound key is channel_id equality filter + created_at sort key.
 * Queries are O(logN + page_size).
 * </p>
 * <p>
 * Ordered by channel_id ASC (order doesn't matter) + created_at DESC.
 * </p>
 */
@Document(collection = "messages")
@CompoundIndex(
        name = "idx_channel_created",
        def = "{'channel_id': 1, 'created_at': -1}"
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /*
     * id is messageId from the event and not a generated ObjectId.
     * this ensures idempotent events in case the same message is processed twice and will be ignored.
     */
    @Id
    private UUID id;

    @Field("channel_id")
    private UUID channelId;
    @Field("author_id")
    private UUID authorId;
    @Field("author_username")
    private String authorUsername;
    private String content;

    @Field("created_at")
    private Instant createdAt;
    @Field("schema_version")
    private int schemaVersion;
}
