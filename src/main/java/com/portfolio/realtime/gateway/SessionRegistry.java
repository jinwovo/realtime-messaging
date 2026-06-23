package com.portfolio.realtime.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks which users currently have at least one live session <strong>on this instance</strong>.
 * A user may hold several connections (multiple tabs/devices), so we reference-count.
 *
 * <p>This is per-instance presence. Cluster-wide presence (e.g. a Redis-backed registry to know
 * if a user is online on <em>any</em> instance) is the prerequisite for the offline-inbox in
 * ADR-0003, and is intentionally out of scope for this milestone.
 */
@Slf4j
@Component
public class SessionRegistry {

    private final ConcurrentMap<String, AtomicInteger> connections = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String user = nameOf(event.getUser());
        if (user != null) {
            connections.computeIfAbsent(user, k -> new AtomicInteger()).incrementAndGet();
            log.debug("connected user={} localSessions={}", user, count(user));
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String user = nameOf(event.getUser());
        if (user != null) {
            connections.computeIfPresent(user, (k, v) -> v.decrementAndGet() <= 0 ? null : v);
            log.debug("disconnected user={} localSessions={}", user, count(user));
        }
    }

    public boolean isConnectedLocally(String userId) {
        return count(userId) > 0;
    }

    public int count(String userId) {
        AtomicInteger c = connections.get(userId);
        return c == null ? 0 : c.get();
    }

    /** Number of distinct users connected to this instance (exposed as a metric, see README). */
    public int localUserCount() {
        return connections.size();
    }

    private static String nameOf(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
