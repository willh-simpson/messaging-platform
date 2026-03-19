package com.messaging.persistenceservice.infrastructure.consumer;

import com.messaging.common.kafka.config.KafkaTopics;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.common.tracing.CorrelationId;
import com.messaging.persistenceservice.domain.model.Message;
import com.messaging.persistenceservice.domain.repository.MessageRepository;
import com.messaging.persistenceservice.config.KafkaConsumerConfig;
import com.messaging.persistenceservice.service.DltHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumes incoming messages produced by Messaging Service and writes to MongoDB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageCreatedEventConsumer {
    private final MessageRepository messageRepo;
    private final DltHandlerService dltHandlerService;

    /**
     * Deserialize and persist received messages to MongoDB.
     * <p>
     * Messages will be retried up to 3 times before failing and being sent to dead letter topic.
     * </p>
     *
     * @param event Incoming message information.
     * @param partition Keyed by channel ID.
     * @param offset Message order. Ensures exactly-once delivery.
     */
    @RetryableTopic(
            attempts = "4", // 1 initial attempt + 3 retries
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_DELAY_VALUE,
            dltTopicSuffix = "-DLT",
            exclude = {
                    DuplicateKeyException.class
            }
    )
    @KafkaListener(
            topics = KafkaTopics.MESSAGES_INBOUND,
            groupId = KafkaConsumerConfig.CONSUMER_GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIncomingMessage(
            MessageCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        if (event.getCorrelationId() != null && !event.getCorrelationId().isBlank()) {
            MDC.put(CorrelationId.MDC_KEY, event.getCorrelationId());
        }

        log.debug(
                "Consuming messageId={}, channelId={}, partition={}, offset={}",
                event.getMessageId(), event.getChannelId(), partition, offset
        );

        try {
            Message message = Message.builder()
                    .id(event.getMessageId())
                    .channelId(event.getChannelId())
                    .authorId(event.getAuthorId())
                    .authorUsername(event.getAuthorUsername())
                    .content(event.getContent())
                    .createdAt(event.getCreatedAt())
                    .schemaVersion(event.getSchemaVersion())
                    .build();
            messageRepo.save(message);

            log.debug("Saved messageId={} to 'messages' document", event.getMessageId());
        } catch (DuplicateKeyException e) {
            // duplicate message can simply be ignored since it has already been saved to message history.
            // the event was successful and this exception does not need to be processed further.

            log.info("Duplicate messageId={}: message already saved and will be skipped", event.getMessageId());
        }

        MDC.remove(CorrelationId.MDC_KEY);
    }

    /**
     * Handles failed messages after all retries have failed.
     * Event information is sent to DltHandlerService.
     *
     * @param event Failed message information.
     * @param topic {@link com.messaging.common.kafka.config.KafkaTopics#MESSAGES_INBOUND inbound messages} topic.
     */
    @DltHandler
    public void handleDeadLetterTopic(
            MessageCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        if (event.getCorrelationId() != null && !event.getCorrelationId().isBlank()) {
            MDC.put(CorrelationId.MDC_KEY, event.getCorrelationId());
        }

        try {
            dltHandlerService.handleDeadLetterTopic(event, topic);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
