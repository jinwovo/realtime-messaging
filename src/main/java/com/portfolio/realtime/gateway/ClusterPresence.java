package com.portfolio.realtime.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Cluster-wide presence: how many live sessions a user holds across <strong>all</strong> instances,
 * tracked as a Redis counter updated on STOMP connect/disconnect. This is what lets the publish path
 * decide whether a direct message can be delivered live or must be parked for offline replay.
 *
 * <p>Complements {@link SessionRegistry} (which is per-instance). Known limitation: a hard crash
 * leaves a stale count — no heartbeat/TTL reaping yet. Graceful disconnects decrement correctly; a
 * production version would expire presence on a client heartbeat.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterPresence {

    private final StringRedisTemplate redis;

    private static String key(String userId) {
        return "presence:" + userId;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String user = nameOf(event.getUser());
        if (user != null) {
            redis.opsForValue().increment(key(user));
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String user = nameOf(event.getUser());
        if (user != null) {
            Long remaining = redis.opsForValue().increment(key(user), -1);
            if (remaining != null && remaining <= 0) {
                redis.delete(key(user));
            }
        }
    }

    public boolean isOnline(String userId) {
        String value = redis.opsForValue().get(key(userId));
        return value != null && Long.parseLong(value) > 0;
    }

    private static String nameOf(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
