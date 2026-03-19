package com.messaging.messagingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis cache for channel membership checks.
 * Prevents DB queries every time a user sends a message because
 * MessageService has to verify user is a member of the target channel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipCacheService {
    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "membership:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Check if membership exists in cache.
     *
     * @param channelId Target channel ID.
     * @param userId Target user ID.
     * @return true if key is present, false if cache miss.
     */
    public boolean isMember(UUID channelId, UUID userId) {
        String key = buildKey(channelId, userId);

        Boolean exists = redisTemplate.hasKey(key);
        boolean hit = Boolean.TRUE.equals(exists);
        log.debug(
                "Membership cache {} for channelId={}, userId={}",
                hit ? "HIT" : "MISS", channelId, userId
        );

        return hit;
    }

    /**
     * Writes confirmed membership into cache.
     *
     * @apiNote Called after PostgreSQL verification confirms user is a channel member.
     * @param channelId Target channel ID.
     * @param userId Target user ID.
     */
    public void cacheMembership(UUID channelId, UUID userId) {
        String key = buildKey(channelId, userId);

        redisTemplate.opsForValue().set(key, "1", CACHE_TTL);
        log.debug(
                "Cached membership for channelId={}, userId={}, ttl={}",
                channelId, userId, CACHE_TTL
        );
    }

    /**
     * Remove membership from cache.
     *
     * @apiNote Called when user leaves a channel or is removed.
     * @param channelId Target channel ID.
     * @param userId Target user ID.
     */
    public void evictMembership(UUID channelId, UUID userId) {
        String key = buildKey(channelId, userId);

        Boolean deleted = redisTemplate.delete(key);
        log.debug(
                "Evicted membership cache for channelId={}, userId={}, found={}",
                channelId, userId, deleted
        );
    }

    /**
     * Create Redis cache key.
     *
     * @param channelId Target channel ID.
     * @param userId Target user ID.
     * @return Key stored in Redis cache.
     */
    private String buildKey(UUID channelId, UUID userId) {
        return KEY_PREFIX + channelId + ":" + userId;
    }
}
