# Demo capture

Automated screenshots of the live demo, driven by [Playwright](https://playwright.dev). The images
in [`../docs/demo`](../docs/demo) are produced by this script and embedded in the project README.

## Prerequisites

Both instances running plus Redis — see the project README's *Prove cross-instance routing* section:

```bash
docker compose up -d redis
SERVER_PORT=8080 INSTANCE_ID=instance-1 ./gradlew bootRun
SERVER_PORT=8081 INSTANCE_ID=instance-2 ./gradlew bootRun
```

## Run

```bash
cd scripts
npm install
npx playwright install chromium
node capture-demo.mjs        # writes instance-1.png + instance-2.png to ../docs/demo
node make-gif.mjs            # writes ../docs/demo/demo.gif (animated demo)
```

Two pages (`alice@8080`, `bob@8081`) connect, a sequence of messages is published to instance-1,
and each instance is screenshotted. Broadcasts reach both instances; the single direct message
reaches only `bob` on instance-2 — which is why the event counters differ (4 vs 5).
