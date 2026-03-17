package com.messaging.messagingservice.service.unit;

import com.messaging.messagingservice.api.dto.request.PublishMessageRequest;
import com.messaging.messagingservice.api.dto.response.MessageAcceptedResponse;
import com.messaging.messagingservice.domain.repository.ChannelMemberViewRepository;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.messagingservice.infrastructure.messaging.producer.MessageCreatedEventProducer;
import com.messaging.messagingservice.security.AuthenticatedUser;
import com.messaging.messagingservice.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService unit tests")
public class MessageServiceTest {
    @Mock
    private MessageCreatedEventProducer messageCreatedEventProducer;
    @Mock
    private ChannelMemberViewRepository channelMemberViewRepo;

    @InjectMocks
    private MessageService messageService;

    private final UUID userId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final String username = "user";

    @BeforeEach
    void setupSecurityContext() {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns 202 Accepted with assigned messageId")
    void publishMessage_success() {
        when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                .thenReturn(true);

        var request = new PublishMessageRequest(channelId, "test content");

        MessageAcceptedResponse response = messageService.publishMessage(request);

        assertThat(response.messageId()).isNotNull();
        assertThat(response.channelId()).isEqualTo(channelId);
        assertThat(response.acceptedAt()).isNotNull();
        assertThat(response.status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("calls producer with channelId as partition key")
    void publishMessage_usesChannelIdAsPartitionKey() {
        when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                .thenReturn(true);

        messageService.publishMessage(new PublishMessageRequest(channelId, "test"));

        // Capture the partition key argument
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageCreatedEventProducer).publish(keyCaptor.capture(), any());

        assertThat(keyCaptor.getValue()).isEqualTo(channelId.toString());
    }

    @Test
    @DisplayName("builds event with correct identity from SecurityContext")
    void publishMessage_eventHasCorrectAuthorAndContent() {
        when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                .thenReturn(true);

        messageService.publishMessage(new PublishMessageRequest(channelId, "Content here"));

        var eventCaptor = ArgumentCaptor.forClass(MessageCreatedEvent.class);
        verify(messageCreatedEventProducer).publish(anyString(), eventCaptor.capture());

        MessageCreatedEvent event = eventCaptor.getValue();

        assertThat(event.getAuthorId()).isEqualTo(userId);
        assertThat(event.getAuthorUsername()).isEqualTo(username);
        assertThat(event.getChannelId()).isEqualTo(channelId);
        assertThat(event.getContent()).isEqualTo("Content here");
        assertThat(event.getMessageId()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("throws SecurityException and never calls producer when not a member")
    void publishMessage_notMember_throwsAndNeverPublishes() {
        when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                .thenReturn(false);

        assertThatThrownBy(() ->
                messageService.publishMessage(new PublishMessageRequest(channelId, "blocked"))
        ).isInstanceOf(SecurityException.class)
                .hasMessageContaining(channelId.toString());

        verifyNoInteractions(messageCreatedEventProducer);
    }

    @Test
    @DisplayName("assigns a unique messageId on every call")
    void publishMessage_eachCallGetsUniqueMessageId() {
        when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                .thenReturn(true);

        var req = new PublishMessageRequest(channelId, "msg");
        var id1 = messageService.publishMessage(req).messageId();
        var id2 = messageService.publishMessage(req).messageId();

        assertThat(id1).isNotEqualTo(id2);
    }
}
