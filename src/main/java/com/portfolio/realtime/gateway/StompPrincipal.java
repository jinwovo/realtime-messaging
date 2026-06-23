package com.portfolio.realtime.gateway;

import java.security.Principal;

/** Minimal {@link Principal} carrying the user id resolved at WebSocket handshake. */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
