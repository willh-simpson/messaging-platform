package com.messaging.persistenceservice.service;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.persistenceservice.domain.model.FailedMessage;
import com.messaging.persistenceservice.domain.repository.FailedMessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles failed messages when Kafka exhausts all retries and sends to Dead Letter Topic.
 */
@Service
@Slf4j
public class DltHandlerService {
    private final FailedMessageRepository failedMessageRepo;

    private final Counter dltMessagesCounter;

    public DltHandlerService(
            FailedMessageRepository failedMessageRepo,
            MeterRegistry meterRegistry
    ) {
        this.failedMessageRepo = failedMessageRepo;

        this.dltMessagesCounter = Counter
                .builder("persistence_dlt_messages_total")
                .description("Total messages that exhausted all Kafka retries and were sent to Dead Letter Topic")
                .register(meterRegistry);
    }

    /**
     * Stores message to 'failed_messages' document and records to meter registry.
     *
     * @param event Message payload from Dead Letter Topic.
     * @param topic DLT topic name.
     */
    public void handleDeadLetterTopic(MessageCreatedEvent event, String topic) {
        failedMessageRepo.save(
                FailedMessage.builder()
                        .id(UUID.randomUUID())
                        .messageId(event.getMessageId())
                        .channelId(event.getChannelId())
                        .authorId(event.getAuthorId())
                        .authorUsername(event.getAuthorUsername())
                        .content(event.getContent())
                        .originalCreatedAt(event.getCreatedAt())
                        .sourceTopic(topic)
                        .correlationId(event.getCorrelationId())
                        .failedAt(Instant.now())
                        .build());

        dltMessagesCounter.increment();

        log.error(
                "Message failed all retries. Stored in 'failed_messages': " +
                        "topic={}, messageId={}, channelId={}, authorId={}, correlationId={} ->" +
                        "GET /api/admin/failed-messages for operator assessment.",
                topic, event.getMessageId(), event.getChannelId(), event.getAuthorId(), event.getCorrelationId()
        );
    }
}
