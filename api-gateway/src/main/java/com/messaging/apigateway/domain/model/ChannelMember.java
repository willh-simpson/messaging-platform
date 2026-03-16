package com.messaging.apigateway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Joins between users and channels.
 */
@Entity
@Table(
        name = "channel_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_channel_member",
                        columnNames = {"channel_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_channel_members_channel", columnList = "channel_id"),
                @Index(name = "idx_channel_members_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public enum MemberRole {
        OWNER, ADMIN, MEMBER
    }
}
