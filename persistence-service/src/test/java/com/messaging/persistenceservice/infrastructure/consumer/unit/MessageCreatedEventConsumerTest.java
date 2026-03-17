package com.messaging.persistenceservice.infrastructure.consumer.unit;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.persistenceservice.domain.model.Message;
import com.messaging.persistenceservice.domain.repository.MessageRepository;
import com.messaging.persistenceservice.infrastructure.messaging.consumer.MessageCreatedEventConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageCreatedEventConsumer Kafka consumer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageCreatedEventConsumer Unit Test")
public class MessageCreatedEventConsumerTest {
    @Mock
    private MessageRepository messageRepo;

    @InjectMocks
    private MessageCreatedEventConsumer messageCreatedEventConsumer;

    private final UUID messageId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID authorId  = UUID.randomUUID();

    private MessageCreatedEvent buildEvent() {
        return MessageCreatedEvent.builder()
                .messageId(messageId)
                .channelId(channelId)
                .authorId(authorId)
                .authorUsername("test user")
                .content("test message")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("saves message document to MongoDB with messageId as _id")
    void consume_validEvent_savesDocumentWithCorrectFields() {
        var event = buildEvent();

        messageCreatedEventConsumer.consumeIncomingMessage(event, 0, 42L);

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepo).save(captor.capture());

        Message saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(messageId);
        assertThat(saved.getChannelId()).isEqualTo(channelId);
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getAuthorUsername()).isEqualTo("test user");
        assertThat(saved.getContent()).isEqualTo("test message");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("silently skips when message is already persisted (duplicate key)")
    void consume_duplicateEvent_doesNotThrow() {
        when(messageRepo.save(any()))
                .thenThrow(new DuplicateKeyException("_id duplicate: " + messageId));

        var event = buildEvent();

        // duplicate message events are not meant to be treated as failures or need to be processed as exceptions
        assertThatNoException().isThrownBy(() ->
                messageCreatedEventConsumer.consumeIncomingMessage(event, 0, 42L)
        );

        verify(messageRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("DLT handler logs and does not throw")
    void handleDlt_logsWithoutThrowing() {
        var event = buildEvent();

        assertThatNoException().isThrownBy(() ->
                messageCreatedEventConsumer.handleDeadLetterTopic(event, "messages.inbound-DLT")
        );

        verifyNoInteractions(messageRepo);
    }
}
