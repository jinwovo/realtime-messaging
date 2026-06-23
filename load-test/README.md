# Load tests

[k6](https://k6.io) scenarios for the realtime-messaging cluster.

## Install k6

- macOS: `brew install k6`
- Windows: `winget install k6 --source winget` (or `choco install k6`)
- Linux / Docker: see the [k6 install docs](https://grafana.com/docs/k6/latest/set-up/install-k6/)

## Scenarios

| File | What it measures | SLO (current target) |
|------|------------------|----------------------|
| `publish-throughput.js` | Ingest rate the cluster accepts | p95 < 200ms, p99 < 500ms, <1% errors |
| `ws-soak.js` | Behaviour holding thousands of live STOMP connections | no leaks, stable session count, zero drops |

## Run

```bash
# 1. start infra + at least one app instance first (see project README)
docker compose up -d
SERVER_PORT=8080 INSTANCE_ID=instance-1 ./gradlew bootRun

# 2. in another shell
k6 run load-test/publish-throughput.js
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
