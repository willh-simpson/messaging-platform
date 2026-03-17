package com.messaging.messagingservice.domain.repository;

import com.messaging.messagingservice.domain.model.ChannelMemberView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChannelMemberViewRepository extends JpaRepository<ChannelMemberView, UUID> {
    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);
}
