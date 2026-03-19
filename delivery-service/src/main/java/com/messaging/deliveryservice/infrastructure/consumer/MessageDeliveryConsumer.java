package com.messaging.deliveryservice.infrastructure.consumer;

import com.messaging.common.kafka.config.KafkaTopics;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.common.tracing.CorrelationId;
import com.messaging.deliveryservice.api.dto.MessageDeliveryPayload;
import com.messaging.deliveryservice.config.KafkaConsumerConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads messages from Kafka and broadcasts to WebSocket subscribers.
 */
@Component
@Slf4j
public class MessageDeliveryConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    private final Counter messagesDeliveredCounter;

    public MessageDeliveryConsumer(
            SimpMessagingTemplate messagingTemplate,
            MeterRegistry meterRegistry
    ) {
        this.messagingTemplate = messagingTemplate;

        this.messagesDeliveredCounter = Counter
                .builder("delivery_messages_broadcast_total")
                .description("Total messages broadcast to WebSocket subscribers")
                .register(meterRegistry);
    }

    /**
     * Deserialize and deliver received messages to WebSocket that broadcasts to subscribers.
     *
     * @param event Incoming message information.
     * @param partition Keyed by channel ID.
     * @param offset Message order. Ensures exactly-once delivery.
     */
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
        if (event.getCorrelationId() != null && !event.getCorrelationId().isBlank()) {
            MDC.put(CorrelationId.MDC_KEY, event.getCorrelationId());
        }

        log.debug("Delivering messageId={}, channelId={}, partition={}, offset={}",
                event.getMessageId(), event.getChannelId(), partition, offset);

        try {
            String destination = "/topic/channels/" + event.getChannelId();
            MessageDeliveryPayload payload = MessageDeliveryPayload.wrapPayload(event);

            messagingTemplate.convertAndSend(destination, payload);
            messagesDeliveredCounter.increment();

            log.debug("Broadcast to {} complete for messageId={}, correlationId={}",
                    destination, event.getMessageId(), event.getCorrelationId());
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
