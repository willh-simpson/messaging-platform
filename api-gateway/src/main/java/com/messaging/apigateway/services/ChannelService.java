package com.messaging.apigateway.services;

import com.messaging.apigateway.api.dto.request.CreateChannelRequest;
import com.messaging.apigateway.api.dto.response.ChannelResponse;
import com.messaging.apigateway.domain.model.Channel;
import com.messaging.apigateway.domain.model.ChannelMember;
import com.messaging.apigateway.domain.repository.ChannelMemberRepository;
import com.messaging.apigateway.domain.repository.ChannelRepository;
import com.messaging.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages channel lifecycle and membership.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {
    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository channelMemberRepo;

    /**
     * Create new channel. Creator will automatically be added to channel as OWNER.
     * Channel names don't need to be unique as they will be identified by their UUID.
     *
     * @param request Name, description, channel type.
     * @param creatorId User creating channel who will be assigned OWNER.
     * @return Newly created channel information.
     */
    @Transactional
    public ChannelResponse createChannel(CreateChannelRequest request, UUID creatorId) {
        Channel channel = Channel.builder()
                .name(request.name())
                .description(request.description())
                .channelType(request.channelType())
                .createdBy(creatorId)
                .build();
        channel = channelRepo.save(channel);

        ChannelMember owner = ChannelMember.builder()
                .channelId(channel.getId())
                .userId(creatorId)
                .role(ChannelMember.MemberRole.OWNER)
                .build();
        channelMemberRepo.save(owner);
        log.info("Created channel {} by user {}", channel.getId(), creatorId);

        return ChannelResponse.toResponse(channel, 1);
    }

    /**
     * Join a channel. Automatically assigned as MEMBER.
     *
     * @apiNote Action is performed by user to be added to channel. Private channels can only be joined via invite.
     * @param channelId Unique channel ID.
     * @param userId Unique user ID.
     */
    @Transactional
    public void joinChannel(UUID channelId, UUID userId) {
        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Channel", "id", channelId
                        )
                );

        if (channel.getChannelType() == Channel.ChannelType.PRIVATE) {
            throw new IllegalStateException("Must be invited to private channel");
        }
        if (channelMemberRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new IllegalArgumentException("User is already a member of this channel");
        }

        ChannelMember member = ChannelMember.builder()
                .channelId(channelId)
                .userId(userId)
                .role(ChannelMember.MemberRole.MEMBER)
                .build();
        channelMemberRepo.save(member);
        log.info("User {} joined channel {}", userId, channelId);
    }

    /**
     * Leave a channel.
     *
     * @apiNote Action is performed by user leaving channel. A channel ban will not access this method.
     * @param channelId Unique channel ID.
     * @param userId Unique user ID.
     */
    @Transactional
    public void leaveChannel(UUID channelId, UUID userId) {
        if (!channelMemberRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new ResourceNotFoundException(
                    "Membership", "channelId+userId", channelId + "+" + userId
            );
        }

        channelMemberRepo.deleteByChannelIdAndUserId(channelId, userId);
        log.info("User {} left channel {}", userId, channelId);
    }

    /**
     * Get pageable list of channels where user is a member.
     *
     * @param userId Unique user id.
     * @param pageable Pagination information (page number, page size, sorting criteria).
     * @return Page of user's channels.
     */
    @Transactional(readOnly = true)
    public Page<ChannelResponse> getChannelsForUser(UUID userId, Pageable pageable) {
        return channelRepo
                .findChannelsByMemberId(userId, pageable)
                .map(channel -> {
                    int memberCount = channelMemberRepo.findByChannelId(channel.getId()).size();

                    return ChannelResponse.toResponse(channel, memberCount);
                });
    }

    /**
     * Get pageable list of public channels.
     *
     * @param pageable Pagination information (page number, page size, sorting criteria).
     * @return Page of public channels.
     */
    @Transactional(readOnly = true)
    public Page<ChannelResponse> getPublicChannels(Pageable pageable) {
        return channelRepo
                .findByChannelType(Channel.ChannelType.PUBLIC, pageable)
                .map(channel -> {
                    int memberCount = channelMemberRepo.findByChannelId(channel.getId()).size();

                    return ChannelResponse.toResponse(channel, memberCount);
                });
    }

    /**
     * Get specified channel. Private channels can only be retrieved by channel members.
     *
     * @param channelId Unique channel ID.
     * @param requestingUserId User ID making request in order to validate membership in private channels.
     * @return Channel information.
     */
    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(UUID channelId, UUID requestingUserId) {
        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Channel", "id", channelId
                        )
                );

        /*
         * private channels are only visible to members
         *
         * exception will return HTTP 404 instead of 401/403 in order to prevent revealing private channel exists
         * and leaking information
         */
        if (channel.getChannelType() == Channel.ChannelType.PRIVATE) {
            if (!channelMemberRepo.existsByChannelIdAndUserId(channelId, requestingUserId)) {
                throw new ResourceNotFoundException(
                        "Channel", "id", channelId
                );
            }
        }

        int memberCount = channelMemberRepo.findByChannelId(channelId).size();

        return ChannelResponse.toResponse(channel, memberCount);
    }
}
