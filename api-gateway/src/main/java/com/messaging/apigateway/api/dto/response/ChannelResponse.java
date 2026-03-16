package com.messaging.apigateway.api.dto.response;

import com.messaging.apigateway.domain.model.Channel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChannelResponse(
        UUID id,

        String name,
        String description,
        Channel.ChannelType channelType,

        UUID createdBy,
        Instant createdAt,

        int memberCount
) {
    public static ChannelResponse toResponse(Channel channel, int memberCount) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .channelType(channel.getChannelType())
                .createdBy(channel.getCreatedBy())
                .createdAt(channel.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
