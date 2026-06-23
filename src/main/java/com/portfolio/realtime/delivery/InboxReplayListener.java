package com.portfolio.realtime.delivery;

import com.portfolio.realtime.notification.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Replays a user's offline inbox the moment they subscribe to their personal queue. Triggering on
 * the <em>subscribe</em> event (not connect) guarantees the subscription is registered with the
 * broker, so replayed messages are actually delivered rather than dropped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxReplayListener {

    private final OfflineInbox inbox;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        if (event.getUser() == null) {
            return;
        }
        String destination = StompHeaderAccessor.wrap(event.getMessage()).getDestination();
        if (destination == null || !destination.endsWith("/queue/notifications")) {
            return;
        }

        String user = event.getUser().getName();
        List<String> pending = inbox.drain(user);
        if (pending.isEmpty()) {
            return;
        }

        log.debug("replaying {} offline message(s) to user={}", pending.size(), user);
        for (String payload : pending) {
            try {
                NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);
                messagingTemplate.convertAndSendToUser(user, "/queue/notifications", message);
            } catch (Exception e) {
                log.error("failed to replay offline message to user={}", user, e);
            }
        }
    }
}
