package com.messaging.persistenceservice.api.controller;

import com.messaging.common.dto.ApiResponse;
import com.messaging.persistenceservice.api.dto.request.AcknowledgeFailedMessageRequest;
import com.messaging.persistenceservice.api.dto.response.FailedMessageResponse;
import com.messaging.persistenceservice.config.ApiMediaTypes;
import com.messaging.persistenceservice.domain.model.FailedMessage;
import com.messaging.persistenceservice.service.FailedMessageAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Exposes endpoints for admin ops DLT assessment.
 */
@RestController
@RequestMapping("/api/admin/failed-messages")
@RequiredArgsConstructor
public class FailedMessageAdminController {
    private final FailedMessageAdminService adminService;

    /**
     * Get failed messages by status.
     *
     * @param status Failed message status (
     *      *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#PENDING PENDING},
     *      *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#ACKNOWLEDGED ACKNOWLEDGED},
     *      *      {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#RESOLVED RESOLVED}
     *      * ).
     * @param pageable Pagination information.
     * @return 200, paged list of failed messages of given status.
     */
    @GetMapping(produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Page<FailedMessageResponse>>> getFailedMessages(
            @RequestParam(defaultValue = "PENDING") FailedMessage.FailedMessageStatus status,
            @PageableDefault(size = 20)Pageable pageable
    ) {
        Page<FailedMessageResponse> messages = adminService.getFailedMessages(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Get failed message by its ID in 'failed_messages' document.
     * @param id Message document ID.
     * @return 200, failed message if it exists.
     */
    @GetMapping(value = "/{id}", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<FailedMessageResponse>> getFailedMessage(@PathVariable UUID id) {
        FailedMessageResponse message = adminService.getFailedMessage(id);

        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Update status of a failed message after operator review.
     *
     * @param id Message document ID.
     * @param request New status, resolution information if issue was resolved.
     * @return 200, new failed message information.
     */
    @PatchMapping(value = "/{id}/status", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<FailedMessageResponse>> acknowledgeFailedMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeFailedMessageRequest request
    ) {
        FailedMessageResponse message = adminService.acknowledgeFailedMessage(id, request);

        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Returns count of
     * {@link com.messaging.persistenceservice.domain.model.FailedMessage.FailedMessageStatus#PENDING PENDING}
     * failures without loading full documents.
     * <p>
     * Fast summary for dashboards and health checks.
     * </p>
     *
     * @return 200, amount of pending messages.
     */
    @GetMapping(value = "/count/pending", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Long>> countPendingMessages() {
        long pendingCount = adminService.countPendingMessages();

        return ResponseEntity.ok(ApiResponse.success(pendingCount));
    }
}
