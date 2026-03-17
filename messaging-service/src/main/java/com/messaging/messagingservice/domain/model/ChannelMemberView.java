package com.messaging.messagingservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only view of 'channel_members' table.
 */
@Entity
@Table(name = "channel_members")
@Immutable
@Getter
public class ChannelMemberView {
    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", updatable = false, nullable = false)
    private UUID channelId;
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;
}
