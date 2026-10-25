// Requires built jars, JDK 25, Docker, running MongoDB on 2026, and Playwright.
// Uses only its own Java processes and a temporary auto-removing PostgreSQL container.
import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { openSync, closeSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = new URL('../docs/screenshots/part3/', import.meta.url);
await mkdir(output, { recursive: true });
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const java = process.env.JAVA_COMMAND || 'java';
const database = `istad-part3-check-${process.pid}`;
const processes = [];
const evidence = { startedAt: new Date().toISOString(), requests: [], screenshots: [] };
let browser;
let databaseStarted = false;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function request(url) {
  const response = await fetch(url, {
    headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(5000) });
  const text = await response.text();
  let body;
  try { body = JSON.parse(text); } catch { body = text; }
  return { url, status: response.status, body };
}

async function until(label, check) {
  const deadline = Date.now() + 120000;
  let last;
  while (Date.now() < deadline) {
    try { if (await check()) { console.log(label); return; } }
    catch (error) { last = error; }
    await delay(1000);
  }
  throw new Error(`Timed out: ${label}. ${last || ''}`);
}

function start(name, jar, args = []) {
  const fd = openSync(fileURLToPath(new URL(`${name}.log`, output)), 'w');
  const child = spawn(java, ['-jar', jar, '--spring.profiles.active=discovery-demo', ...args],
    { cwd: root, stdio: ['ignore', fd, fd] });
  closeSync(fd);
  processes.push(child);
  return child;
}

async function stop(child, signal = 'SIGTERM') {
  if (child.exitCode !== null || child.signalCode !== null) return;
  const exited = new Promise(resolve => child.once('exit', resolve));
  child.kill(signal);
  await Promise.race([exited, delay(10000)]);
  if (child.exitCode === null && child.signalCode === null) {
    child.kill('SIGKILL');
    await exited;
  }
}

async function businessInstances() {
  const result = await request('http://localhost:8761/eureka/apps/BUSINESS-SERVICE');
  if (result.status === 404) return [];
  assert.equal(result.status, 200);
  const instances = result.body.application.instance;
  return (Array.isArray(instances) ? instances : [instances]).filter(i => i.status === 'UP');
}

try {
  execFileSync('docker', ['run', '--rm', '-d', '--name', database, '-p', '15433:5432',
    '-e', 'POSTGRES_DB=istad_order', '-e', 'POSTGRES_USER=istad',
    '-e', 'POSTGRES_PASSWORD=istad', 'postgres:17'], { stdio: 'pipe' });
  databaseStarted = true;
  await until('Temporary PostgreSQL ready', async () => {
    try { execFileSync('docker', ['exec', database, 'pg_isready', '-U', 'istad'],
      { stdio: 'pipe' }); return true; } catch { return false; }
  });
  start('eureka', 'microservices/discovery-server/build/libs/discovery-server-1.0.0.jar');
  await until('Eureka ready', async () => (await request('http://localhost:8761/actuator/health')).body.status === 'UP');
  const jar = 'microservices/business-service/business-service-main/build/libs/business-service-main-1.0.0.jar';
  const first = start('business-8082', jar, ['--server.port=8082', '--eureka.instance.ip-address=127.0.0.1']);
  const second = start('business-8182', jar, ['--server.port=8182', '--eureka.instance.ip-address=127.0.0.1']);
  for (const port of [8082, 8182]) {
    await until(`Business ${port} healthy`, async () =>
      (await request(`http://localhost:${port}/actuator/health`)).body.status === 'UP');
  }
  start('order', 'microservices/order-service/order-service-main/build/libs/order-service-main-1.0.0.jar',
    ['--spring.datasource.url=jdbc:postgresql://localhost:15433/istad_order',
     '--eureka.instance.ip-address=127.0.0.1']);
  await until('Order healthy', async () => (await request('http://localhost:8081/actuator/health')).body.status === 'UP');
  await until('Two business instances registered UP', async () => (await businessInstances()).length === 2);
  await until('Order registered UP', async () => {
    const result = await request('http://localhost:8761/eureka/apps/ORDER-SERVICE');
    if (result.status !== 200) return false;
    const instances = result.body.application.instance;
    return (Array.isArray(instances) ? instances : [instances]).some(i => i.status === 'UP');
  });
  const endpoint = 'http://localhost:8081/api/discovery/business';
  await until('Order resolves business through Eureka', async () => (await request(endpoint)).status === 200);
  browser = await chromium.launch({ headless: true,
    ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}) });
  const page = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  async function capture(name, url) {
    const response = await page.goto(url);
    await page.screenshot({ path: fileURLToPath(new URL(name, output)), fullPage: true });
    evidence.screenshots.push({ file: name, url, status: response.status() });
    return response;
  }
  await capture('01-eureka-two-business-instances.png', 'http://localhost:8761');
  evidence.initialRegistry = await businessInstances();
  const ports = new Set();
  for (let i = 0; i < 12; i++) {
    const response = await page.goto(endpoint);
    const body = await response.json();
    assert.equal(response.status(), 200);
    assert.equal(body.service, 'business-service');
    assert.ok(['8082', '8182'].includes(body.port));
    evidence.requests.push({ phase: 'two-instances', url: endpoint, status: response.status(), body });
    if (!ports.has(body.port)) {
      const name = `02-discovered-business-${body.port}.png`;
      await page.screenshot({ path: fileURLToPath(new URL(name, output)) });
      evidence.screenshots.push({ file: name, url: endpoint, status: response.status() });
    }
    ports.add(body.port);
  }
  assert.deepEqual([...ports].sort(), ['8082', '8182']);
  const baseline = await request('http://localhost:8081/api/hardcoded/business');
  assert.equal(baseline.status, 404);
  evidence.baselineDisabled = baseline;
  console.log('12 real order-to-business calls reached both instances; historical endpoint disabled');

  const failedAt = Date.now();
  await stop(first, 'SIGKILL');
  let consecutive = 0;
  await until('Failed 8082 evicted; calls converge on 8182', async () => {
    const result = await request(endpoint);
    evidence.requests.push({ phase: 'failure-convergence', elapsedMs: Date.now() - failedAt, ...result });
    consecutive = result.status === 200 && result.body.port === '8182' ? consecutive + 1 : 0;
    const instances = await businessInstances();
    return consecutive >= 6 && instances.length === 1 && String(instances[0].port.$) === '8182';
  });
  evidence.convergenceMs = Date.now() - failedAt;
  evidence.survivingRegistry = await businessInstances();
  const survivor = await capture('03-surviving-business-8182.png', endpoint);
  assert.equal(survivor.status(), 200);
  assert.equal((await survivor.json()).port, '8182');
  await capture('04-eureka-after-failure.png', 'http://localhost:8761');
  await stop(second);
  await until('No business instances; caller returns 503', async () =>
    (await businessInstances()).length === 0 && (await request(endpoint)).status === 503);
  const unavailable = await capture('05-no-business-available.png', endpoint);
  assert.equal(unavailable.status(), 503);
  evidence.allUnavailable = await request(endpoint);
  evidence.completedAt = new Date().toISOString();
  await writeFile(new URL('results.json', output), JSON.stringify(evidence, null, 2) + '\n');
  console.log(`PASS: evidence saved to ${fileURLToPath(output)}`);
} finally {
  if (browser) await browser.close();
  for (const child of processes.reverse()) await stop(child);
  if (databaseStarted) execFileSync('docker', ['stop', database], { stdio: 'pipe' });
}
