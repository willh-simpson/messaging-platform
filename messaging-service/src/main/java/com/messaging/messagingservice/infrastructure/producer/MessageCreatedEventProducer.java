package com.messaging.messagingservice.infrastructure.producer;

import com.messaging.common.kafka.config.KafkaTopics;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes messages to Kafka.
 */
@Component
@Slf4j
public class MessageCreatedEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Counter messagesPublishedCounter;

    public MessageCreatedEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;

        this.messagesPublishedCounter = Counter
                .builder("messaging_messages_published_total")
                .description("Total number of messages successfully published to Kafka")
                .register(meterRegistry);
    }

    /**
     * Publish message to "messages.inbound" topic on specified partition key.
     *
     * @param partitionKey Channel ID as partition. Keeps strict ordering of messages per channel.
     * @param event Message payload.
     */
    public void publish(
            String partitionKey, MessageCreatedEvent event
    ) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaTopics.MESSAGES_INBOUND,
                partitionKey,
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Kafka publish failed: messageId={}, channelId={}, correlationId={}, error={}",
                        event.getMessageId(), event.getChannelId(), event.getCorrelationId(), ex.getMessage(),
                        ex
                );
            } else {
                messagesPublishedCounter.increment();

                RecordMetadata metadata = result.getRecordMetadata();
                log.debug(
                        "Published messageId={}: topic={}, partition={}, offset={}, correlationId={}",
                        event.getMessageId(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        event.getCorrelationId()
                );
            }
        });
    }
}
