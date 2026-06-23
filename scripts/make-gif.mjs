// Records the live demo into an animated GIF (no ffmpeg required): drives instance-1, publishes a
// sequence of messages, screenshots each step, and encodes the frames with gifenc.
//
//   cd scripts && npm install && npx playwright install chromium && node make-gif.mjs
//
// Writes ../docs/demo/demo.gif. Requires instance-1 running on :8080 (see project README).
import { chromium } from 'playwright';
import { setTimeout as sleep } from 'node:timers/promises';
import { PNG } from 'pngjs';
import gifenc from 'gifenc';
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const { GIFEncoder, quantize, applyPalette } = gifenc;

const OUT = process.env.OUT || fileURLToPath(new URL('../docs/demo/demo.gif', import.meta.url));
const A = process.env.A_URL || 'http://localhost:8080';

const decode = (buf) => {
  const png = PNG.sync.read(buf);
  return { data: new Uint8Array(png.data), width: png.width, height: png.height };
};

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 820, height: 980 }, deviceScaleFactor: 1 });
const page = await ctx.newPage();
await page.goto(A, { waitUntil: 'load' });
await page.evaluate(() => document.fonts.ready);

await page.fill('#user', 'alice');
await page.click('#connect');
await page.waitForSelector('.status.live', { timeout: 10000 });

const frames = [];
const cap = async () => frames.push(decode(await page.screenshot()));

const publish = (userId, type, content) =>
  page.evaluate(async (b) => {
    await fetch('/api/notifications', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(b),
    });
  }, { userId, type, content });

await sleep(500);
await cap();

const seq = [
  [null, 'SYSTEM', 'instance-2 joined the cluster'],
  [null, 'CHAT', 'anyone up for the 3pm sync?'],
  [null, 'DEPLOY', 'shipping the realtime layer \u{1F680}'],
  ['alice', 'ALERT', 'your build #482 passed ✅'],
  [null, 'CHAT', 'cross-instance delivery confirmed \u{1F389}'],
  [null, 'CHAT', 'p99 delivery 45ms under load'],
];
for (const [userId, type, content] of seq) {
  await publish(userId, type, content);
  await sleep(170);
  await cap();
  await sleep(230);
  await cap();
}
await sleep(600);
await cap();

await browser.close();

const { width, height } = frames[0];
const gif = GIFEncoder();
frames.forEach((f, i) => {
  const palette = quantize(f.data, 256);
  const index = applyPalette(f.data, palette);
  const delay = i === 0 ? 800 : i === frames.length - 1 ? 1800 : 190;
  gif.writeFrame(index, width, height, { palette, delay });
});
gif.finish();
writeFileSync(OUT, Buffer.from(gif.bytes()));
console.log('wrote', OUT, '-', frames.length, 'frames', `${width}x${height}`);
