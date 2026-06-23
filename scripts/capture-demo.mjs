// Drives the running demo with Playwright and captures the portfolio screenshots in docs/demo.
//
// Prereqs: both instances running (:8080, :8081) + Redis up (see project README), then:
//   cd scripts && npm install && npx playwright install chromium && node capture-demo.mjs
//
// Two pages (alice@8080, bob@8081) connect; a sequence of messages is published to instance-1.
// Broadcasts reach both instances; the one direct message reaches only bob on instance-2 — which
// is why the event counters differ (4 vs 5) in the two screenshots.
import { chromium } from 'playwright';
import { setTimeout as sleep } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';
import { mkdir } from 'node:fs/promises';

const OUT = process.env.OUT_DIR || fileURLToPath(new URL('../docs/demo', import.meta.url));
const A = process.env.A_URL || 'http://localhost:8080';
const B = process.env.B_URL || 'http://localhost:8081';

async function connect(page, url, user) {
  await page.goto(url, { waitUntil: 'load' });
  await page.fill('#user', user);
  await page.click('#connect');
  await page.waitForSelector('.status.live', { timeout: 10000 });
}

async function publish(target, type, content) {
  const res = await fetch(`${A}/api/notifications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: target, type, content }),
  });
  if (res.status !== 202) throw new Error('publish failed: ' + res.status);
}

await mkdir(OUT, { recursive: true });

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1320, height: 1040 }, deviceScaleFactor: 2 });
const pageA = await ctx.newPage();
const pageB = await ctx.newPage();

await connect(pageA, A, 'alice');
await connect(pageB, B, 'bob');

const seq = [
  [null, 'SYSTEM', 'instance-2 joined the cluster'],
  [null, 'CHAT', 'anyone up for the 3pm sync?'],
  [null, 'DEPLOY', 'shipping the realtime layer \u{1F680}'],
  ['bob', 'ALERT', 'build #482 passed ✅'],
  [null, 'CHAT', 'cross-instance delivery confirmed \u{1F389}'],
];
for (const [target, type, content] of seq) {
  await publish(target, type, content);
  await sleep(350);
}

await sleep(700);
await pageA.evaluate(() => document.fonts.ready);
await pageB.evaluate(() => document.fonts.ready);
await sleep(300);

await pageA.screenshot({ path: `${OUT}/instance-1.png`, fullPage: true });
await pageB.screenshot({ path: `${OUT}/instance-2.png`, fullPage: true });

await browser.close();
console.log('captured instance-1.png + instance-2.png to', OUT);
