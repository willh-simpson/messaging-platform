package com.messaging.persistenceservice.api.dto.request;

import com.messaging.persistenceservice.domain.model.FailedMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcknowledgeFailedMessageRequest(
        @NotNull(message = "Status is required")
        FailedMessage.FailedMessageStatus status,

        @Size(max = 1000, message = "Resolution note must be under 1000 characters")
        String resolutionNote
) {
}
