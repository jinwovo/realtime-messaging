package com.portfolio.realtime.config;

import com.portfolio.realtime.gateway.UserHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket entry point.
 *
 * <p>We use the in-memory <em>simple broker</em> on purpose: it only knows about sessions
 * connected to <strong>this</strong> JVM. The moment we run more than one instance, a
 * {@code /user/**} message published on instance A cannot reach a session held by instance B.
 * Closing that gap is the whole point of {@code RedisNotificationSubscriber} — see ADR-0002.
 *
 * <p>Payload (de)serialization is left to Boot 4's autoconfigured Jackson 3 converter, which
 * handles Java records and {@code java.time} types out of the box.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Raw WebSocket (no SockJS) keeps the k6 load tests and the demo client simple.
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new UserHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }
}
