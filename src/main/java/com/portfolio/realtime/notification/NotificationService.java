package com.portfolio.realtime.notification;

import com.portfolio.realtime.delivery.OfflineInbox;
import com.portfolio.realtime.gateway.ClusterPresence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Decides how a notification is delivered:
 * <ul>
 *   <li>broadcasts and direct messages to an <em>online</em> recipient go straight to the
 *       cluster-wide Redis channel for live delivery;</li>
 *   <li>a direct message to a recipient who is offline on every instance is parked in the
 *       {@link OfflineInbox} and replayed when they reconnect (ADR-0003).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationPublisher publisher;
    private final ClusterPresence presence;
    private final OfflineInbox inbox;
    private final ObjectMapper objectMapper;

    public void send(NotificationMessage message) {
        if (message.isBroadcast() || presence.isOnline(message.userId())) {
            publisher.publish(message);
        } else {
            inbox.store(message.userId(), serialize(message));
            log.debug("recipient {} offline on all instances; parked for replay", message.userId());
        }
    }

    private String serialize(NotificationMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize notification " + message.id(), e);
        }
    }
}
