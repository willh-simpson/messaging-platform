package com.messaging.messagingservice.service;

import com.messaging.messagingservice.api.dto.request.PublishMessageRequest;
import com.messaging.messagingservice.api.dto.response.MessageAcceptedResponse;
import com.messaging.messagingservice.domain.repository.ChannelMemberViewRepository;
import com.messaging.messagingservice.infrastructure.event.MessageCreatedEvent;
import com.messaging.messagingservice.infrastructure.messaging.producer.MessageCreatedEventProducer;
import com.messaging.messagingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Manage message publishing and re-establishing user authentication per request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private final ChannelMemberViewRepository channelMemberRepo;
    private final MessageCreatedEventProducer messageCreatedEventProducer;

    /**
     * Publish message to Kafka if user is authorized to post to specified channel.
     *
     * @apiNote Message events are partitioned by channel ID. This keep messages synchronous per channel.
     * @param request Channel ID and message content.
     * @return Processed message content.
     */
    public MessageAcceptedResponse publishMessage(PublishMessageRequest request) {
        AuthenticatedUser caller = getAuthenticatedUser();

        if (!channelMemberRepo.existsByChannelIdAndUserId(request.channelId(), caller.userId())) {
            throw new SecurityException(
                    "User is not a member of channel " + request.channelId()
            );
        }

        UUID messageId = UUID.randomUUID();
        Instant now = Instant.now();
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(messageId)
                .channelId(request.channelId())
                .authorId(caller.userId())
                .authorUsername(caller.username())
                .content(request.content())
                .createdAt(now)
                .build();

        messageCreatedEventProducer.publish(request.channelId().toString(), event);

        return MessageAcceptedResponse.builder()
                .messageId(messageId)
                .channelId(request.channelId())
                .acceptedAt(now)
                .status("ACCEPTED")
                .build();
    }

    /**
     * Get user auth details stored in SecurityContext.
     *
     * @return User information if exists in SecurityContext.
     */
    private AuthenticatedUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }

        return user;
    }
}
