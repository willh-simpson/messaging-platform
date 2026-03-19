package com.messaging.messagingservice.service;

import com.messaging.common.tracing.CorrelationId;
import com.messaging.messagingservice.api.dto.request.PublishMessageRequest;
import com.messaging.messagingservice.api.dto.response.MessageAcceptedResponse;
import com.messaging.messagingservice.domain.repository.ChannelMemberViewRepository;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.messagingservice.infrastructure.producer.MessageCreatedEventProducer;
import com.messaging.messagingservice.security.AuthenticatedUser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Manage message publishing and re-establishing user authentication per request.
 */
@Service
@Slf4j
public class MessageService {
    private final ChannelMemberViewRepository channelMemberRepo;
    private final MembershipCacheService membershipCacheService;

    private final MessageCreatedEventProducer messageCreatedEventProducer;

    private final Counter membershipCacheHitCounter;
    private final Counter membershipCacheMissCounter;

    public MessageService(
            ChannelMemberViewRepository channelMemberRepo,
            MembershipCacheService membershipCacheService,
            MessageCreatedEventProducer messageCreatedEventProducer,
            MeterRegistry meterRegistry
    ) {
        this.channelMemberRepo = channelMemberRepo;
        this.membershipCacheService = membershipCacheService;
        this.messageCreatedEventProducer = messageCreatedEventProducer;

        this.membershipCacheHitCounter = Counter
                .builder("messaging_membership_cache_hits_total")
                .description("Membership checks served from Redis cache")
                .register(meterRegistry);
        this.membershipCacheMissCounter = Counter
                .builder("messaging_membership_cache_misses_total")
                .description("Membership checks requiring PostgreSQL query")
                .register(meterRegistry);
    }

    /**
     * Publish message to Kafka if user is authorized to post to specified channel.
     *
     * @apiNote Message events are partitioned by channel ID. This keep messages synchronous per channel.
     * @param request Channel ID and message content.
     * @return Processed message content.
     */
    public MessageAcceptedResponse publishMessage(PublishMessageRequest request) {
        AuthenticatedUser caller = getAuthenticatedUser();

        // user sending messages needs to be cached in order to stop DB queries with every message
        checkMembershipCache(request.channelId(), caller.userId());

        UUID messageId = UUID.randomUUID();
        Instant now = Instant.now();
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(messageId)
                .channelId(request.channelId())
                .authorId(caller.userId())
                .authorUsername(caller.username())
                .content(request.content())
                .createdAt(now)
                .correlationId(MDC.get(CorrelationId.MDC_KEY))
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
     * Checks if user for specified channel has been cached in Redis AND if user is a channel member.
     * If user is a channel member but not cached then they are added to the cache.
     *
     * @param channelId Target channel ID.
     * @param userId Target user ID.
     */
    private void checkMembershipCache(UUID channelId, UUID userId) {
        if (membershipCacheService.isMember(channelId, userId)) {
            membershipCacheHitCounter.increment();

            return;
        }
        membershipCacheMissCounter.increment();

        if (!channelMemberRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new SecurityException("User is not a member of channel " + channelId);
        }

        membershipCacheService.cacheMembership(channelId, userId);
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
