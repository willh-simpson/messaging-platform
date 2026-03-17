package com.messaging.messagingservice.infrastructure.messaging.producer;

import com.messaging.messagingservice.infrastructure.config.KafkaConfig;
import com.messaging.messagingservice.infrastructure.event.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class MessageCreatedEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
                KafkaConfig.MESSAGES_INBOUND_TOPIC,
                partitionKey,
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Kafka publish failed: messageId={}, channelId={}, error={}",
                        event.getMessageId(), event.getChannelId(), ex.getMessage(), ex
                );
            } else {
                RecordMetadata metadata = result.getRecordMetadata();
                log.debug(
                        "Published messageId={}: topic={}, partition={}, offset={}",
                        event.getMessageId(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }
}
