package com.portfolio.realtime.notification;

import tools.jackson.databind.ObjectMapper;
import com.portfolio.realtime.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a notification onto the cluster-wide Redis channel. The producer does not care
 * which instance the recipient is connected to — every instance receives the message and
 * decides locally whether it has a session to deliver to.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void publish(NotificationMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redis.convertAndSend(RedisConfig.NOTIFICATION_CHANNEL, payload);
            log.debug("published id={} target={}", message.id(),
                    message.isBroadcast() ? "<broadcast>" : message.userId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish notification " + message.id(), e);
        }
    }
}
