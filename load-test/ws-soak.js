// Connection soak test: hold N concurrent STOMP-over-WebSocket subscribers and watch that the
// cluster keeps them all up with stable resource use.
//
//   k6 run load-test/ws-soak.js
//   WS_URL=ws://localhost:8080/ws k6 run load-test/ws-soak.js
//
// k6 has no STOMP library, so we hand-build the frames. Uses the k6 v2
// `k6/experimental/websockets` API (the legacy `k6/ws` was removed in v2).
import { WebSocket } from 'k6/experimental/websockets';
import { Counter } from 'k6/metrics';

const messagesReceived = new Counter('stomp_messages_received');

const NUL = String.fromCharCode(0);
const frame = (cmd, h, body = '') =>
  `${cmd}\n${Object.entries(h).map(([k, v]) => `${k}:${v}`).join('\n')}\n\n${body}${NUL}`;

const HOST = __ENV.WS_URL || 'ws://localhost:8080/ws';

export const options = {
  scenarios: {
    soak: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { target: 1000, duration: '30s' },
        { target: 1000, duration: '30s' }, // hold
        { target: 0, duration: '10s' },
      ],
    },
  },
};

export default function () {
  const ws = new WebSocket(`${HOST}?user=u${__VU}`);

  ws.addEventListener('open', () => {
    ws.send(frame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '10000,10000' }));
  });

  ws.addEventListener('message', (e) => {
    const data = e.data;
    if (typeof data !== 'string') return;
    if (data.startsWith('CONNECTED')) {
      ws.send(frame('SUBSCRIBE', { id: 'b' + __VU, destination: '/topic/notifications' }));
    } else if (data.startsWith('MESSAGE')) {
      messagesReceived.add(1);
    }
  });
}
