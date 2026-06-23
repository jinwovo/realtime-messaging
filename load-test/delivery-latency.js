// End-to-end delivery-latency test (the headline realtime metric).
//
//   k6 run load-test/delivery-latency.js
//
// 200 subscribers hold STOMP connections split across both instances; a publisher fires broadcasts
// at instance-1. Each subscriber records (receivedAt - createdAt) — server stamp vs. client receipt
// on the same host, so the delta is true publish->fan-out->deliver latency. Uses the k6 v2
// `k6/experimental/websockets` API (the legacy `k6/ws` was removed).
import { WebSocket } from 'k6/experimental/websockets';
import { Trend, Counter } from 'k6/metrics';
import http from 'k6/http';

const deliveryLatency = new Trend('delivery_latency_ms', true);
const delivered = new Counter('messages_delivered');

const NUL = String.fromCharCode(0);
const frame = (cmd, h, body = '') =>
  `${cmd}\n${Object.entries(h).map(([k, v]) => `${k}:${v}`).join('\n')}\n\n${body}${NUL}`;

const SUBS = ['ws://localhost:8080/ws', 'ws://localhost:8081/ws'];
const PUB = 'http://localhost:8080/api/notifications';

export const options = {
  scenarios: {
    subscribers: {
      executor: 'constant-vus',
      vus: 200,
      duration: '40s',
      exec: 'subscriber',
    },
    publisher: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 5,
      maxVUs: 20,
      startTime: '5s', // let subscribers connect first
      exec: 'publisher',
    },
  },
  thresholds: {
    delivery_latency_ms: ['p(95)<150', 'p(99)<400'],
    messages_delivered: ['count>0'],
  },
};

export function subscriber() {
  const ws = new WebSocket(SUBS[__VU % SUBS.length] + '?user=sub-' + __VU);

  ws.addEventListener('open', () => {
    ws.send(frame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '0,0' }));
  });

  ws.addEventListener('message', (e) => {
    const data = e.data;
    if (typeof data !== 'string') return;
    if (data.startsWith('CONNECTED')) {
      ws.send(frame('SUBSCRIBE', { id: 's' + __VU, destination: '/topic/notifications' }));
    } else if (data.startsWith('MESSAGE')) {
      const body = (data.split('\n\n')[1] || '').replace(/\0$/, '');
      try {
        const n = JSON.parse(body);
        const lat = Date.now() - new Date(n.createdAt).getTime();
        if (lat >= 0 && lat < 60000) {
          deliveryLatency.add(lat);
          delivered.add(1);
        }
      } catch (_) {
        /* ignore non-JSON frames */
      }
    }
  });
}

export function publisher() {
  http.post(PUB, JSON.stringify({ type: 'LOADTEST', content: 'ping' }), {
    headers: { 'Content-Type': 'application/json' },
  });
}
