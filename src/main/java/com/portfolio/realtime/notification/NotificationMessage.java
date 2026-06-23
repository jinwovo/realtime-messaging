package com.portfolio.realtime.notification;

import java.time.Instant;

/**
 * A notification flowing through the system.
 *
 * @param id        server-generated unique id (used later for idempotent / at-least-once delivery)
 * @param userId    target user; {@code null} or blank means broadcast to every subscriber
 * @param type      application-defined category, e.g. {@code "CHAT"}, {@code "ALERT"}
 * @param content   human-readable payload
 * @param createdAt server timestamp the notification was accepted
 */
public record NotificationMessage(
        String id,
        String userId,
        String type,
        String content,
        Instant createdAt
) {
    public boolean isBroadcast() {
        return userId == null || userId.isBlank();
    }
}
