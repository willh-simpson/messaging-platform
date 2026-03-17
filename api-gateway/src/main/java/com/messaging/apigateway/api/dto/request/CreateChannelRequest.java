package com.messaging.apigateway.api.dto.request;

import com.messaging.apigateway.domain.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChannelRequest(
        @NotBlank(message = "Channel name is required")
        @Size(min = 2, max = 100, message = "Channel name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description must be less than 500 characters")
        String description,

        @NotNull(message = "Channel type is required")
        Channel.ChannelType channelType
) {
}
