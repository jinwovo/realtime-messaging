package com.portfolio.realtime.gateway;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves a {@link Principal} from the {@code ?user=} query parameter at handshake time,
 * so {@code convertAndSendToUser(user, ...)} can target a specific person without full auth.
 *
 * <p>This is a deliberate demo shortcut. In production this class would be replaced by a
 * JWT/session-validating handshake interceptor — see the "Security" note in the README.
 */
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String user = queryParam(request.getURI().getRawQuery(), "user");
        if (user == null || user.isBlank()) {
            user = "anon-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return new StompPrincipal(user);
    }

    private static String queryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
