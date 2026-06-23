// Ingest throughput test: how many publishes/sec can the cluster accept within latency SLOs.
//
//   k6 run load-test/publish-throughput.js
//   BASE_URL=http://localhost:8080 k6 run load-test/publish-throughput.js
//
// TODO(milestone-3): publish through a load balancer in front of 2+ instances and record the
// before/after of each optimization in the README.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    throughput: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 500,
      stages: [
        { target: 200, duration: '30s' },
        { target: 1000, duration: '1m' },
        { target: 1000, duration: '1m' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200', 'p(99)<500'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const payload = JSON.stringify({ type: 'LOAD', content: 'load-test message' }); // broadcast
  const res = http.post(`${BASE}/api/notifications`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'status is 202': (r) => r.status === 202 });
}
