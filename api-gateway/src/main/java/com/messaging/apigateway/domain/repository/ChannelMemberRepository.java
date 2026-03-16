package com.messaging.apigateway.domain.repository;

import com.messaging.apigateway.domain.model.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {
    Optional<ChannelMember> findByChannelIdAndUserId(UUID channelId, UUID userId);
    List<ChannelMember> findByChannelId(UUID channelId);

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    void deleteByChannelIdAndUserId(UUID channelId, UUID userID);
}
