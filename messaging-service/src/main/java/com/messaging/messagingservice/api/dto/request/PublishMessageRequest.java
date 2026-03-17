package com.messaging.messagingservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PublishMessageRequest(
        @NotNull(message = "Channel ID is required")
        UUID channelId,

        @NotBlank(message = "Content is required")
        @Size(max = 4000, message = "Message content must be under 4000 characters")
        String content
) {
}
