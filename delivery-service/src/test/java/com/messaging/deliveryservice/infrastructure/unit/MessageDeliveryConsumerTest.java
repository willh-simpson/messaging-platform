package com.messaging.deliveryservice.infrastructure.unit;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.deliveryservice.api.dto.MessageDeliveryPayload;
import com.messaging.deliveryservice.infrastructure.consumer.MessageDeliveryConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for MessageDeliveryConsumer Kafka consumer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageDeliveryConsumer unit test")
public class MessageDeliveryConsumerTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageDeliveryConsumer consumer;

    private final UUID messageId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID authorId  = UUID.randomUUID();

    @Test
    @DisplayName("broadcasts to correct STOMP destination for the channel")
    void consume_broadcastsToChannelDestination() {
        var event = buildEvent();

        consumer.consume(event, 2, 99L);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/channels/" + channelId),
                any(MessageDeliveryPayload.class)
        );
    }

    @Test
    @DisplayName("payload contains correct message fields, excludes internal event fields")
    void consume_payloadMapsEventFieldsCorrectly() {
        var event = buildEvent();

        consumer.consume(event, 0, 0L);

        var payloadCaptor = ArgumentCaptor.forClass(MessageDeliveryPayload.class);
        verify(messagingTemplate).convertAndSend(
                any(String.class),
                payloadCaptor.capture()
        );

        MessageDeliveryPayload payload = payloadCaptor.getValue();
        assertThat(payload.messageId()).isEqualTo(messageId);
        assertThat(payload.channelId()).isEqualTo(channelId);
        assertThat(payload.authorID()).isEqualTo(authorId);
        assertThat(payload.authorUsername()).isEqualTo("alice");
        assertThat(payload.content()).isEqualTo("Hello, world!");
        assertThat(payload.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("each channel gets its own isolated destination")
    void consume_differentChannels_differentDestinations() {
        UUID channelA = UUID.randomUUID();
        UUID channelB = UUID.randomUUID();

        consumer.consume(buildEventForChannel(channelA), 0, 1L);
        consumer.consume(buildEventForChannel(channelB), 1, 2L);

        var destCaptor = ArgumentCaptor.forClass(String.class);
        // Capture both calls
        verify(messagingTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(destCaptor.capture(), any(MessageDeliveryPayload.class));

        var destinations = destCaptor.getAllValues();
        assertThat(destinations).containsExactlyInAnyOrder(
                "/topic/channels/" + channelA,
                "/topic/channels/" + channelB
        );
        assertThat(destinations.get(0)).isNotEqualTo(destinations.get(1));
    }

    private MessageCreatedEvent buildEvent() {
        return buildEventForChannel(channelId);
    }

    private MessageCreatedEvent buildEventForChannel(UUID channel) {
        return MessageCreatedEvent.builder()
                .messageId(messageId)
                .channelId(channel)
                .authorId(authorId)
                .authorUsername("alice")
                .content("Hello, world!")
                .createdAt(Instant.now())
                .build();
    }
}
