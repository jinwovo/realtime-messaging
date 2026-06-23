# ADR-0002 — Redis Pub/Sub for cross-instance routing

- **Status:** Accepted (Milestone 1)
- **Date:** 2026-06

## Context

With more than one instance, the STOMP simple broker can only reach sessions on its own JVM. A
message produced on `instance-2` for a user connected to `instance-1` would never arrive. We need a
mechanism for an event produced anywhere to reach the instance that actually holds the recipient.

## Decision

Publish every notification to a shared **Redis Pub/Sub** channel. Every instance subscribes; on
receipt, each instance delivers **only to its locally-connected sessions** (`RedisNotificationSubscriber`
+ `SessionRegistry`).

## Rationale

- Already operating Redis (presence, future inbox) — no new moving part.
- Pub/Sub latency is sub-millisecond on a local network; negligible vs. WebSocket RTT.
- The "broadcast to all instances, deliver locally" model is simple to reason about and makes
  exactly-once delivery to a single-connection user trivial.

## Consequences

- **Every instance sees every message.** Fine for moderate volume; at high fan-out this is wasted
  work on instances with no matching recipient. Milestone 3 will measure where this hurts.
- **Redis Pub/Sub is fire-and-forget.** A message published while an instance is briefly
  disconnected from Redis is lost. Durability is handled separately (ADR-0003), not by this channel.
- Horizontal scaling of the app is now stateless w.r.t. routing — any instance can accept any
  publish.

## Alternatives considered

- **RabbitMQ STOMP relay** (`StompBrokerRelay`): a real broker that natively routes user
  destinations across instances. More capable (acks, durability) but adds a broker to operate and
  couples delivery to it. Reconsider if per-message guarantees outgrow Redis.
- **Kafka as the routing bus:** great for durability/replay (and is on the roadmap for *ingestion*),
  but consumer-group semantics fit "process once", not "deliver to whichever instance holds the
  socket". Wrong tool for the fan-out hop.
- **Sticky sessions + directory lookup:** route the publish to the one instance holding the user.
  Removes redundant fan-out but needs a consistent presence directory and a routing layer — more
  complexity than Milestone 1 warrants.
