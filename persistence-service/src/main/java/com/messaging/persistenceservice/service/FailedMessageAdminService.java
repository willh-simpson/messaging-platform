package com.messaging.persistenceservice.service;

import com.messaging.common.exception.ResourceNotFoundException;
import com.messaging.persistenceservice.api.dto.request.AcknowledgeFailedMessageRequest;
import com.messaging.persistenceservice.api.dto.response.FailedMessageResponse;
import com.messaging.persistenceservice.domain.model.FailedMessage;
import com.messaging.persistenceservice.domain.repository.FailedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Ops interface for managing DLT failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FailedMessageAdminService {
    private final FailedMessageRepository failedMessageRepo;

    /**
     * Find failed messages based on status. Newest messages first.
     *
     * @param status Failed message status (
     *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#PENDING PENDING},
     *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#ACKNOWLEDGED ACKNOWLEDGED},
     *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#RESOLVED RESOLVED}
     * ).
     * @param pageable Pagination information.
     * @return Paged list of failed messages.
     */
    public Page<FailedMessageResponse> getFailedMessages(
            FailedMessage.FailedMessageStatus status,
            Pageable pageable
    ) {
        return failedMessageRepo
                .findByStatusOrderByFailedAtDesc(status, pageable)
                .map(FailedMessageResponse::toResponse);
    }

    /**
     * Get failed message by its stored document ID.
     *
     * @throws ResourceNotFoundException If ID is not present in document.
     * @param id ID in 'failed_messages' document.
     * @return Failed message information, if it exists.
     */
    public FailedMessageResponse getFailedMessage(UUID id) {
        return failedMessageRepo
                .findById(id)
                .map(FailedMessageResponse::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException("FailedMessage", "id", id)
                );
    }

    /**
     * Acknowledge failed message and provide resolution notes if the issue was resolved.
     *
     * @throws ResourceNotFoundException If ID is not present in document.
     * @param id ID in 'failed_messages' document.
     * @param request Failed message status update and resolution notes if issue is resolved.
     * @return Updated failed message information.
     */
    public FailedMessageResponse acknowledgeFailedMessage(UUID id, AcknowledgeFailedMessageRequest request) {
        FailedMessage existingMessage = failedMessageRepo
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("FailedMessage", "id", id)
                );

        FailedMessage updatedMessage = FailedMessage.builder()
                .id(existingMessage.getId())
                .messageId(existingMessage.getMessageId())
                .channelId(existingMessage.getChannelId())
                .authorId(existingMessage.getAuthorId())
                .authorUsername(existingMessage.getAuthorUsername())
                .content(existingMessage.getContent())
                .originalCreatedAt(existingMessage.getOriginalCreatedAt())
                .sourceTopic(existingMessage.getSourceTopic())
                .correlationId(existingMessage.getCorrelationId())
                .failedAt(existingMessage.getFailedAt())
                .status(request.status())
                .acknowledgedAt(Instant.now())
                .resolutionNote(request.resolutionNote())
                .build();
        failedMessageRepo.save(updatedMessage);
        log.info(
                "FailedMessage {} acknowledged: status={}, note={}",
                id, request.status(), request.resolutionNote()
        );

        return FailedMessageResponse.toResponse(updatedMessage);
    }

    /**
     * Get amount of failed messages that are unacknowledged
     * ({@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#PENDING PENDING}).
     *
     * @return Amount of unacknowledged messages.
     */
    public long countPendingMessages() {
        return failedMessageRepo.countByStatus(FailedMessage.FailedMessageStatus.PENDING);
    }
}
