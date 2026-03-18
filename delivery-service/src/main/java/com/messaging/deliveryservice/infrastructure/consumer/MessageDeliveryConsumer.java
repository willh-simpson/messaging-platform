package com.messaging.deliveryservice.infrastructure.consumer;

import com.messaging.common.kafka.config.KafkaTopics;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.deliveryservice.api.dto.MessageDeliveryPayload;
import com.messaging.deliveryservice.config.KafkaConsumerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads messages from Kafka and broadcasts to WebSocket subscribers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = KafkaTopics.MESSAGES_INBOUND,
            groupId = KafkaConsumerConfig.CONSUMER_GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            MessageCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.debug("Delivering messageId={}, channelId={}, partition={}, offset={}",
                event.getMessageId(), event.getChannelId(), partition, offset);

        String destination = "/topic/channels/" + event.getChannelId();
        MessageDeliveryPayload payload = MessageDeliveryPayload.wrapPayload(event);

        messagingTemplate.convertAndSend(destination, payload);

        log.debug("Broadcast to {} complete for messageId={}",
                destination, event.getMessageId());
    }
}
