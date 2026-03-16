package com.messaging.apigateway.domain.repository;

import com.messaging.apigateway.domain.model.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    @Query("""
        SELECT c FROM Channel c
        WHERE EXISTS (
            SELECT cm FROM ChannelMember cs
            WHERE cm.channelId = c.id
            AND cm.userId = :userId
        )
    """)
    Page<Channel> findChannelsByMemberId(@Param("userId") UUID userId, Pageable pageable);
    Page<Channel> findByChannelType(Channel.ChannelType channelType, Pageable pageable);

    boolean existsByName(String name);
}
