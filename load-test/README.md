# Load tests

[k6](https://k6.io) scenarios for the realtime-messaging cluster.

## Install k6

- macOS: `brew install k6`
- Windows: `choco install k6`, or grab the binary from [k6 releases](https://github.com/grafana/k6/releases)
- Linux / Docker: see the [k6 install docs](https://grafana.com/docs/k6/latest/set-up/install-k6/)

> The WebSocket scripts use the `k6/experimental/websockets` module (k6 ≥ v0.40; the legacy
> `k6/ws` was removed in k6 v2).

## Scenarios

| File | What it measures | Latest result (dev machine) |
|------|------------------|-----------------------------|
| `publish-throughput.js` | Ingest rate the cluster accepts | 1,000 req/s · p99 13 ms · 0 errors |
| `delivery-latency.js` | End-to-end publish → fan-out → deliver, across both instances | p95 33 ms · p99 45 ms @ ~1.7k msg/s |
| `ws-soak.js` | Holding many concurrent STOMP connections | 1,000 connections held, stable |

## Run

```bash
# 1. start infra + at least one app instance first (see project README)
docker compose up -d
SERVER_PORT=8080 INSTANCE_ID=instance-1 ./gradlew bootRun

# 2. in another shell
k6 run load-test/publish-throughput.js
k6 run load-test/delivery-latency.js
k6 run load-test/ws-soak.js

# point at a different host/port
BASE_URL=http://localhost:8081 k6 run load-test/publish-throughput.js
WS_URL=ws://localhost:8081/ws   k6 run load-test/ws-soak.js
```

## Method (Milestone 3)

The goal is a **before/after** story, not a single number:

1. Establish a baseline against one instance.
2. Scale to two instances behind a load balancer; re-measure.
3. Drive the soak test while publishing, and record end-to-end delivery latency and drop rate.
4. Publish the headline figures — and the bottleneck found at each step — in the project README.
