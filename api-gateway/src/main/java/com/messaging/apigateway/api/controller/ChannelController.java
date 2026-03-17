package com.messaging.apigateway.api.controller;

import com.messaging.apigateway.api.dto.request.CreateChannelRequest;
import com.messaging.apigateway.api.dto.response.ChannelResponse;
import com.messaging.apigateway.config.ApiMediaTypes;
import com.messaging.apigateway.security.JwtTokenProvider;
import com.messaging.apigateway.service.ChannelService;
import com.messaging.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public endpoints for channel management.
 */
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Create new channel. Requires authentication.
     *
     * @param request Requesting channel information.
     * @param authHeader User auth information.
     * @return 201, new channel.
     */
    @PostMapping(produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @Valid @RequestBody CreateChannelRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        ChannelResponse channel = channelService.createChannel(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(channel));
    }

    /**
     * List public channels. No authentication required.
     *
     * @param pageable Pagination information.
     * @return 200, paged list of public channels.
     */
    @GetMapping(produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Page<ChannelResponse>>> getPublicChannels(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<ChannelResponse> channels = channelService.getPublicChannels(pageable);

        return ResponseEntity.ok(ApiResponse.success(channels));
    }

    /**
     * List channels where authenticated user is a member.
     *
     * @param authHeader User auth information.
     * @param pageable Pagination information.
     * @return 200, paged list of user's channels.
     */
    @GetMapping(value = "/me", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Page<ChannelResponse>>> getMyChannels(
            @RequestHeader("Authorization") String authHeader,
            @PageableDefault(size = 20, sort = "creatdAt") Pageable pageable
    ) {
        UUID userId = extractUserId(authHeader);
        Page<ChannelResponse> channels = channelService.getChannelsForUser(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(channels));
    }

    /**
     * Get specific channel by ID. Authentication required for private channels.
     *
     * @param channelId Unique channel ID.
     * @param authHeader User auth information.
     * @return 200, channel if user has permission to view.
     */
    @GetMapping(value = "/{channel_id}", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(
            @PathVariable("channel_id") UUID channelId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        ChannelResponse channel = channelService.getChannelById(channelId, userId);

        return ResponseEntity.ok(ApiResponse.success(channel));
    }

    /**
     * Join a public channel. Not used for joining private channels.
     *
     * @param channelId Unique channel ID.
     * @param authHeader User auth information.
     * @return 200, no extra content.
     */
    @PostMapping(value = "/{channel_id}/join", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Void>> joinChannel(
            @PathVariable("channel_id") UUID channelId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        channelService.joinChannel(channelId, userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Leave a channel.
     *
     * @param channelId Unique channel ID.
     * @param authHeader User auth information.
     * @return 200, no extra information.
     */
    @DeleteMapping(value = "/{channel_id}/leave", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<Void>> leaveChannel(
            @PathVariable("channel_id") UUID channelId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        channelService.leaveChannel(channelId, userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get user ID from JWT.
     * @param authHeader Authentication header from HTTP request.
     * @return User ID.
     */
    private UUID extractUserId(String authHeader) {
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;

        return tokenProvider.getUserIdFromToken(token);
    }
}
