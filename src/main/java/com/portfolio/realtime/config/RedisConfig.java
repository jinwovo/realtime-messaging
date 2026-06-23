package com.portfolio.realtime.config;

import com.portfolio.realtime.notification.RedisNotificationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the cluster-wide notification fan-out channel. Every instance subscribes to the same
 * Redis channel, so a message published on any instance is observed by all of them — see ADR-0002.
 */
@Configuration
public class RedisConfig {

    public static final String NOTIFICATION_CHANNEL = "notifications";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisNotificationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(NOTIFICATION_CHANNEL));
        return container;
    }
}
