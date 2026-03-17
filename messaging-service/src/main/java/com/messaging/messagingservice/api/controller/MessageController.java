package com.messaging.messagingservice.api.controller;

import com.messaging.common.dto.ApiResponse;
import com.messaging.messagingservice.api.dto.request.PublishMessageRequest;
import com.messaging.messagingservice.api.dto.response.MessageAcceptedResponse;
import com.messaging.messagingservice.config.ApiMediaTypes;
import com.messaging.messagingservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose endpoints for MessageService.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    /**
     * Accepts message and publishes to Kafka.
     *
     * @param request Channel ID and message content.
     * @return 202, processed message content.
     */
    @PostMapping(produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<MessageAcceptedResponse>> publishMessage(
            @Valid @RequestBody PublishMessageRequest request
    ) {
        MessageAcceptedResponse response = messageService.publishMessage(request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response));
    }
}
