# Architecture

## Goals & non-goals

**Goals**
- Deliver real-time messages correctly across a horizontally-scaled cluster.
- Make every design trade-off explicit and measurable.
- Keep the local developer loop simple (one `docker compose up`, one `bootRun`).

**Non-goals (for now)**
- AuthN/AuthZ beyond a demo handshake (see README security note).
- Multi-region / geo-replication.
- Message history / search (this is delivery, not storage).

## Components

| Component | Responsibility |
|-----------|----------------|
| `NotificationController` | HTTP ingress; validates, stamps id + timestamp, returns 202. |
| `NotificationPublisher` | Serializes and publishes to the cluster-wide Redis channel. |
| `RedisMessageListenerContainer` | Subscribes this instance to the Redis channel. |
| `RedisNotificationSubscriber` | Receives every message; delivers to local sessions only. |
| `SessionRegistry` | Per-instance presence, reference-counted over STOMP connect/disconnect. |
| `WebSocketConfig` | STOMP broker + endpoint; pins JSON converter to the app `ObjectMapper`. |
| `UserHandshakeHandler` | Resolves a `Principal` from `?user=` at handshake. |
| `NotificationService` | Routes each message: publish live vs. park in the offline inbox. |
| `ClusterPresence` | Redis-counter presence across all instances; gates live-vs-offline. |
| `OfflineInbox` | Durable per-user Redis inbox for messages received while offline. |
| `InboxReplayListener` | Drains & replays the inbox when a user subscribes to their queue. |

## Message flow

```mermaid
sequenceDiagram
    participant P as Producer (HTTP)
    participant I2 as instance-2 (publisher)
    participant R as Redis channel
    participant I1 as instance-1 (has User A)
    participant A as User A (WebSocket)

    P->>I2: POST /api/notifications {userId: A}
    I2->>R: PUBLISH notifications {…}
    R-->>I1: message
    R-->>I2: message
    Note over I2: A not connected locally → no-op
    Note over I1: A connected locally → deliver
    I1->>A: STOMP /user/queue/notifications
```

## Why this is the hard part

The in-memory STOMP "simple broker" only knows sessions on its own JVM. Two instances each have a
partial view of who is online. Redis Pub/Sub turns "deliver to user X" into "tell every instance,
let whichever one holds X deliver" — at the cost of every instance seeing every message (fine for
moderate fan-out, revisited under load in Milestone 3). The alternative — a shared external broker
that natively routes user destinations (e.g. RabbitMQ STOMP relay) — is evaluated in ADR-0002.

## Delivery semantics

- **Direct, recipient online (any instance):** delivered live via the Redis channel.
- **Direct, recipient offline everywhere:** parked in a durable Redis inbox and replayed when they
  next subscribe (Milestone 2) — at-least-once for the offline case.
- **Broadcast:** delivered to currently-connected subscribers only (not persisted).
- **Remaining hardening (ADR-0003):** atomic Lua drain + client acks for strict no-duplicate
  semantics, and presence reaping via heartbeat TTL.

## Observability

Micrometer exports to Prometheus at `/actuator/prometheus`. Planned custom metrics: active sessions
per instance (`SessionRegistry.localUserCount()`), publish counter, and an end-to-end delivery
latency timer (producer stamp → client receive) sampled via the load-test client.
