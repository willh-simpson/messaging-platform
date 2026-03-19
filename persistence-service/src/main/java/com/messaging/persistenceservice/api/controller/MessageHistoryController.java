package com.messaging.persistenceservice.api.controller;

import com.messaging.common.dto.ApiResponse;
import com.messaging.persistenceservice.api.dto.response.MessageResponse;
import com.messaging.persistenceservice.config.ApiMediaTypes;
import com.messaging.persistenceservice.security.AuthenticatedUser;
import com.messaging.persistenceservice.service.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Expose message history endpoints for MessageHistoryService.
 */
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class MessageHistoryController {
    private final MessageHistoryService messageHistoryService;

    /**
     * Gets paginated message history. Newest messages first.
     *
     * @param channelId Channel ID to query.
     * @param pageable Pagination parameters.
     * @return 200, paged list of messages.
     */
    @GetMapping(value = "/{channel_id}/messages", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessageHistory(
            @PathVariable("channel_id") UUID channelId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        AuthenticatedUser caller = getAuthenticatedUser();

        Page<MessageResponse> messages = messageHistoryService.getMessageHistory(
                channelId, caller.userId(), pageable
        );

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Get user auth information so message history queries can be performed.
     *
     * @return User auth information, if user is authenticated.
     */
    private AuthenticatedUser getAuthenticatedUser() {
        return (AuthenticatedUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
