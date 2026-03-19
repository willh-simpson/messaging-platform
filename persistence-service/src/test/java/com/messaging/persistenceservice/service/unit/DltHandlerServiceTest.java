package com.messaging.persistenceservice.service.unit;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.persistenceservice.domain.model.FailedMessage;
import com.messaging.persistenceservice.domain.repository.FailedMessageRepository;
import com.messaging.persistenceservice.service.DltHandlerService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for DltHandlerService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DltHandlerService unit tests")
public class DltHandlerServiceTest {
    @Mock
    private FailedMessageRepository failedMessageRepo;

    @Spy
    private SimpleMeterRegistry meterRegistry;

    private DltHandlerService dltHandlerService;

    private final UUID messageId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final String correlationId = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        dltHandlerService = new DltHandlerService(
                failedMessageRepo,
                meterRegistry
        );
    }

    @Test
    @DisplayName("persists a FailedMessage document with PENDING status")
    void handleDeadLetterTopic_persistsFailedMessageWithPendingStatus() {
        dltHandlerService.handleDeadLetterTopic(buildEvent(), "messages.inbound-DLT");
        var captor = ArgumentCaptor.forClass(FailedMessage.class);

        verify(failedMessageRepo).save(captor.capture());

        FailedMessage saved = captor.getValue();

        assertThat(saved.getMessageId()).isEqualTo(messageId);
        assertThat(saved.getChannelId()).isEqualTo(channelId);
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getAuthorUsername()).isEqualTo("test user");
        assertThat(saved.getContent()).isEqualTo("test content");
        assertThat(saved.getSourceTopic()).isEqualTo("messages.inbound-DLT");
        assertThat(saved.getStatus()).isEqualTo(FailedMessage.FailedMessageStatus.PENDING);
        assertThat(saved.getFailedAt()).isNotNull();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("increments DLT Prometheus counter on each failure")
    void handleDeadLetterTopic_incrementsDltCounter() {
        dltHandlerService.handleDeadLetterTopic(buildEvent(), "messages.inbound-DLT");
        dltHandlerService.handleDeadLetterTopic(buildEvent(), "messages.inbound-DLT");

        double count = meterRegistry.counter("persistence_dlt_messages_total").count();

        assertThat(count).isEqualTo(2.0);
    }

    @Test
    @DisplayName("preserves correlationId from original event")
    void handleDeadLetterTopic_preservesCorrelationId() {
        dltHandlerService.handleDeadLetterTopic(buildEvent(), "messages.inbound-DLT");
        var captor = ArgumentCaptor.forClass(FailedMessage.class);

        verify(failedMessageRepo).save(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isEqualTo(correlationId);
    }

    private MessageCreatedEvent buildEvent() {
        return MessageCreatedEvent.builder()
                .messageId(messageId)
                .channelId(channelId)
                .authorId(authorId)
                .authorUsername("test user")
                .content("test content")
                .createdAt(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}
