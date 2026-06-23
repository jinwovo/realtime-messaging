# ADR-0003 — Delivery-guarantee roadmap (offline inbox)

- **Status:** Proposed (Milestone 2)
- **Date:** 2026-06

## Context

Today delivery is **at-most-once**: `RedisNotificationSubscriber` drops a targeted message if the
recipient is not connected to any instance. For notifications that must survive a brief disconnect
(the common case on mobile), this is insufficient.

## Decision (proposed)

Introduce a durable **per-user inbox** in Redis and move to **at-least-once** delivery with
client-side de-duplication by message id.

Sketch:

1. On publish, attempt live delivery as today **and** append to the recipient's inbox
   (`ZADD inbox:{userId} score=timestamp member={id,payload}`) with a TTL.
2. On client connect, after subscribing, the client requests a replay; the server drains inbox
   entries newer than the client's last-acked id and removes acked entries.
3. The client de-dupes by `id` (a message may arrive both live and via replay).

## Consequences

- Requires a client→server **ack** frame and a "last seen id" per device — this is why ADR-0001
  chose a bidirectional protocol.
- Introduces cluster-wide presence questions (is the user online *anywhere*?) and inbox TTL /
  trimming policy to bound storage.
- Moves the system from fire-and-forget to a model with clear, testable delivery semantics — the
  load tests in Milestone 3 will assert **zero loss** under churn.

## Open questions

- TTL vs. explicit trim on ack — or both?
- Ordering guarantees across replay + live streams (timestamp tiebreak by id?).
- Do we need exactly-once, or is at-least-once + idempotent client sufficient? (Current bet: the
  latter, which is dramatically cheaper.)
