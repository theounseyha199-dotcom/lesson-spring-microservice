// Run once with "success", then again with "moved" after moving business to 8182.
// Requires Playwright (PLAYWRIGHT_MODULE can point to its index.mjs).
import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';

const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const phase = process.argv[2];
assert.ok(['success', 'moved'].includes(phase), 'Usage: node scripts/capture-part1.mjs success|moved');
const output = new URL('../docs/screenshots/part1/', import.meta.url);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true,
  ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}) });
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 },
    extraHTTPHeaders: { Accept: 'application/json' } });
  const checks = phase === 'success'
    ? [['01-business-8082', 'http://localhost:8082/api/businesses/instance', 200, '8082'],
       ['02-order-hardcoded-success', 'http://localhost:8081/api/hardcoded/business', 200, '8082']]
    : [['03-business-moved-8182', 'http://localhost:8182/api/businesses/instance', 200, '8182'],
       ['04-order-hardcoded-failure', 'http://localhost:8081/api/hardcoded/business', 503, null]];
  const results = [];
  for (const [name, url, expectedStatus, port] of checks) {
    const response = await page.goto(url);
    assert.equal(response.status(), expectedStatus);
    const contentType = response.headers()['content-type'] || '';
    const text = await response.text();
    const body = contentType.includes('application/json') ? JSON.parse(text) : text;
    if (port) {
      assert.equal(body.port, port);
      assert.equal(body.service, 'business-service');
    } else {
      if (typeof body === 'string') assert.match(body, /status=503/);
      else assert.equal(body.status, 503);
    }
    await page.screenshot({ path: new URL(`${name}.png`, output).pathname });
    results.push({ url, status: response.status(), contentType, body, screenshot: `${name}.png` });
  }
  const evidence = { capturedAt: new Date().toISOString(), phase, results };
  await writeFile(new URL(`${phase}.json`, output), JSON.stringify(evidence, null, 2) + '\n');
  console.log(JSON.stringify(evidence, null, 2));
} finally {
  await browser.close();
}
