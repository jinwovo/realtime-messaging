package com.portfolio.realtime.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Durable per-user inbox for direct messages that arrived while the recipient was offline on every
 * instance. Backed by a Redis list with a TTL so storage stays bounded; replayed on reconnect by
 * {@link InboxReplayListener}.
 *
 * <p>Increment 1 (ADR-0003): at-least-once for the offline case. {@link #drain} is LRANGE+DELETE,
 * which races with a message stored concurrently mid-drain; a hardened version would use an atomic
 * Lua drain plus client acks. Documented, not yet hardened.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineInbox {

    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    private static String key(String userId) {
        return "inbox:" + userId;
    }

    public void store(String userId, String payload) {
        String k = key(userId);
        redis.opsForList().rightPush(k, payload);
        redis.expire(k, TTL);
        log.debug("stored offline message for user={}", userId);
    }

    /** Returns all pending payloads for the user (oldest first) and clears the inbox. */
    public List<String> drain(String userId) {
        String k = key(userId);
        List<String> pending = redis.opsForList().range(k, 0, -1);
        if (pending != null && !pending.isEmpty()) {
            redis.delete(k);
        }
        return pending == null ? List.of() : pending;
    }
}
