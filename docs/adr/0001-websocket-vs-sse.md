# ADR-0001 — WebSocket (STOMP) vs Server-Sent Events

- **Status:** Accepted
- **Date:** 2026-06

## Context

The system needs to push messages from server to client in real time. Two mainstream browser-native
options: Server-Sent Events (SSE) and WebSocket. The choice constrains the protocol, the broker
model, and how load testing is done.

## Decision

Use **WebSocket with the STOMP sub-protocol**.

## Rationale

| Factor | SSE | WebSocket + STOMP |
|--------|-----|-------------------|
| Direction | Server → client only | Bidirectional |
| Subscriptions / topics | Manual, app-defined | Built into STOMP (`/topic`, `/user/queue`) |
| User-targeted destinations | Hand-rolled | `convertAndSendToUser` out of the box |
| Reconnect / heartbeat | Browser auto-reconnect | STOMP heartbeats + client logic |
| Proxy friendliness | Plain HTTP (easy) | Needs `Upgrade` support |

STOMP gives first-class topic and per-user destinations, which map directly onto the routing problem
this project is about. Bidirectionality also leaves room for client→server acks, needed by the
at-least-once delivery design in ADR-0003.

## Consequences

- We depend on `spring-boot-starter-websocket` and a STOMP broker abstraction.
- The simple in-memory broker is per-instance only, which forces the Redis fan-out in ADR-0002.
- Load tests must speak STOMP frames over raw WebSocket (handled in `load-test/`), which is more
  involved than curling an SSE stream — an accepted cost.

## Alternatives considered

- **SSE + a separate POST channel for client→server.** Simpler infra, but we would re-implement
  topic routing and user addressing that STOMP already provides.
- **Raw WebSocket, no STOMP.** Maximum control, but we'd hand-build framing, subscriptions, and
  user-destination resolution. Not worth it at this stage.
