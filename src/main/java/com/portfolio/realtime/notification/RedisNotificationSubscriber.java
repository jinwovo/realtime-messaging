package com.portfolio.realtime.notification;

import tools.jackson.databind.ObjectMapper;
import com.portfolio.realtime.gateway.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Receives every notification off the Redis channel and delivers it to sessions connected
 * to <strong>this</strong> instance only. Because all instances run the identical logic on
 * the same message, a uniquely-targeted user connected to exactly one instance is delivered
 * to exactly once; a broadcast reaches each subscriber once via its own instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            NotificationMessage notification = objectMapper.readValue(body, NotificationMessage.class);
            deliverLocally(notification);
        } catch (Exception e) {
            log.error("Failed to handle notification from Redis", e);
        }
    }

    private void deliverLocally(NotificationMessage n) {
        if (n.isBroadcast()) {
            messagingTemplate.convertAndSend("/topic/notifications", n);
            return;
        }
        if (sessionRegistry.isConnectedLocally(n.userId())) {
            messagingTemplate.convertAndSendToUser(n.userId(), "/queue/notifications", n);
        }
        // else: recipient is not on this instance. If they are offline on EVERY instance the
        // message is currently dropped — durable offline delivery is the next milestone (ADR-0003).
    }
}
