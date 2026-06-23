// Connection soak test: hold N concurrent STOMP-over-WebSocket subscribers and watch memory,
// active-session metrics, and (later) delivery latency under a steady publish stream.
//
//   k6 run load-test/ws-soak.js
//   WS_URL=ws://localhost:8080/ws k6 run load-test/ws-soak.js
//
// k6 has no STOMP library, so we hand-build the frames — which doubles as proof of understanding
// the protocol. Uses the stable k6/ws API (k6/experimental/websockets is the future alternative).
import ws from 'k6/ws';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const messagesReceived = new Counter('stomp_messages_received');

export const options = {
  scenarios: {
    soak: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { target: 1000, duration: '1m' },
        { target: 5000, duration: '2m' },
        { target: 5000, duration: '3m' }, // hold
        { target: 0, duration: '30s' },
      ],
    },
  },
};

const NULL = String.fromCharCode(0); // STOMP frames are terminated by the NUL octet
const HOST = __ENV.WS_URL || 'ws://localhost:8080/ws';
const HOLD_MS = 60000;

function frame(command, headers, body = '') {
  const h = Object.entries(headers).map(([k, v]) => `${k}:${v}`).join('\n');
  return `${command}\n${h}\n\n${body}${NULL}`;
}

export default function () {
  const user = `u${__VU}`;
  const res = ws.connect(`${HOST}?user=${user}`, {}, (socket) => {
    socket.on('open', () => {
      socket.send(frame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '10000,10000' }));
    });

    socket.on('message', (msg) => {
      if (msg.startsWith('CONNECTED')) {
        socket.send(frame('SUBSCRIBE', { id: `b-${__VU}`, destination: '/topic/notifications' }));
        socket.send(frame('SUBSCRIBE', { id: `q-${__VU}`, destination: '/user/queue/notifications' }));
      } else if (msg.startsWith('MESSAGE')) {
        messagesReceived.add(1);
        // TODO(milestone-3): parse the JSON body's createdAt and record end-to-end latency
        // (now - createdAt) into a Trend to assert p99 delivery latency.
      }
    });

    socket.setTimeout(() => socket.close(), HOLD_MS);
  });

  check(res, { 'ws handshake 101': (r) => r && r.status === 101 });
}
