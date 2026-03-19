package com.messaging.persistenceservice.service;

import com.messaging.common.exception.ResourceNotFoundException;
import com.messaging.persistenceservice.api.dto.response.MessageResponse;
import com.messaging.persistenceservice.domain.repository.ChannelMemberViewRepository;
import com.messaging.persistenceservice.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Serves paginated message history from MongoDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageHistoryService {
    private final MessageRepository messageRepo;
    private final ChannelMemberViewRepository channelMemberViewRepo;

    /**
     * Get paged message history of messages in a given channel.
     *
     * @apiNote Requesting user must be a member of the channel to view message history.
     *
     * @param channelId Channel ID to query.
     * @param requestingUserId Authenticated user making request.
     * @param pageable Pagination parameters.
     * @return Paginated message history for channel. Newest messages first.
     */
    public Page<MessageResponse> getMessageHistory(
            UUID channelId, UUID requestingUserId, Pageable pageable
    ) {
        if (!channelMemberViewRepo.existsByChannelIdAndUserId(channelId, requestingUserId)) {
            throw new ResourceNotFoundException("Channel", "id", channelId);
        }

        /*
         * forcing sort by createdAt DESC because clients are not allowed
         * to sort message history arbitrarily.
         * it's unnecessary and would add needless complexity.
         */
        Pageable enforcedPage = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return messageRepo
                .findByChannelIdOrderByCreatedAtDesc(channelId, enforcedPage)
                .map(MessageResponse::toResponse);
    }
}
