// Requires built jars, JDK 25, Docker, the MongoDB image, and Playwright.
// Uses only its own Java processes and isolated auto-removing database containers.
import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { openSync, closeSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = new URL('../docs/screenshots/part4/', import.meta.url);
await mkdir(output, { recursive: true });
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const java = process.env.JAVA_COMMAND || 'java';
const database = `istad-part4-check-${process.pid}`;
const processes = [];
const evidence = { startedAt: new Date().toISOString(), observations: [], screenshots: [] };
let browser;
let databaseStarted = false;
const mongo = `istad-part4-mongo-${process.pid}`;
let mongoStarted = false;
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

const endpoint = 'http://localhost:8081/api/discovery/business';
const health = port => `http://localhost:${port}/actuator/health`;
async function instances(application = 'BUSINESS-SERVICE') {
  const result = await request(`http://localhost:8761/eureka/apps/${application}`);
  if (result.status === 404) return [];
  assert.equal(result.status, 200);
  const list = result.body.application.instance;
  return Array.isArray(list) ? list : [list];
}
async function hasStatus(status) {
  const list = await instances();
  return list.length === 1 && list[0].status === status;
}
async function observe(phase, since, port = 8082) {
  const result = { phase, capturedAt: new Date().toISOString(), elapsedMs: Date.now() - since,
    registry: await instances(), caller: await request(endpoint) };
  try { result.health = await request(health(port)); }
  catch { result.health = { unavailable: true }; }
  evidence.observations.push(result);
  console.log(`${phase}: caller ${result.caller.status}, registry ${result.registry.map(i => i.status)}`);
  return result;
}
const businessJar = 'microservices/business-service/business-service-main/build/libs/business-service-main-1.0.0.jar';
function startBusiness(name, port = 8082, enabled = true) {
  return start(name, businessJar, [`--server.port=${port}`,
    '--eureka.instance.ip-address=127.0.0.1',
    `--eureka.client.healthcheck.enabled=${enabled}`,
    '--spring.mongodb.uri=mongodb://localhost:12026/part4?serverSelectionTimeoutMS=1000&connectTimeoutMS=1000&socketTimeoutMS=1000&heartbeatFrequencyMS=1000']);
}
async function waitHealthy(port = 8082) {
  await until(`Business ${port} healthy and registered UP`, async () =>
    (await request(health(port))).body.status === 'UP' && await hasStatus('UP') &&
    (await request(endpoint)).status === 200);
}

try {
  execFileSync('docker', ['run', '--rm', '-d', '--name', database, '-p', '15433:5432',
    '-e', 'POSTGRES_DB=istad_order', '-e', 'POSTGRES_USER=istad',
    '-e', 'POSTGRES_PASSWORD=istad', 'postgres:17'], { stdio: 'pipe' });
  databaseStarted = true;
  execFileSync('docker', ['run', '--rm', '-d', '--name', mongo, '-p', '12026:27017',
    'mongodb/mongodb-community-server:latest'], { stdio: 'pipe' });
  mongoStarted = true;
  await until('Temporary PostgreSQL ready', async () => {
    try { execFileSync('docker', ['exec', database, 'pg_isready', '-U', 'istad'],
      { stdio: 'pipe' }); return true; } catch { return false; }
  });
  start('eureka', 'microservices/discovery-server/build/libs/discovery-server-1.0.0.jar');
  await until('Eureka ready', async () => (await request(health(8761))).body.status === 'UP');
  start('order', 'microservices/order-service/order-service-main/build/libs/order-service-main-1.0.0.jar',
    ['--spring.datasource.url=jdbc:postgresql://localhost:15433/istad_order',
     '--eureka.instance.ip-address=127.0.0.1']);
  await until('Order healthy and registered UP', async () =>
    (await request(health(8081))).body.status === 'UP' &&
    (await instances('ORDER-SERVICE')).some(i => i.status === 'UP'));
  const initial = await observe('before-business-starts', Date.now());
  assert.equal(initial.registry.length, 0);
  assert.equal(initial.caller.status, 503);
  browser = await chromium.launch({ headless: true,
    ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}) });
  const page = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  async function capture(file, url) {
    const response = await page.goto(url);
    await page.screenshot({ path: fileURLToPath(new URL(file, output)), fullPage: true });
    evidence.screenshots.push({ file, url, status: response.status() });
  }
  let changedAt = Date.now();
  let business = startBusiness('business-health-enabled');
  await waitHealthy();
  await observe('started-health-enabled', changedAt);
  await capture('01-started-up.png', 'http://localhost:8761');

  changedAt = Date.now();
  execFileSync('docker', ['pause', mongo], { stdio: 'pipe' });
  await until('Dependency unavailable: Actuator DOWN, Eureka DOWN, caller 503', async () =>
    (await request(health(8082))).body.status === 'DOWN' && await hasStatus('DOWN') &&
    (await request(endpoint)).status === 503);
  const down = await observe('dependency-down-health-enabled', changedAt);
  assert.equal(down.health.status, 503);
  assert.equal((await request('http://localhost:8082/api/businesses/instance')).status, 200);
  await capture('02-dependency-down-health.png', health(8082));
  await capture('03-dependency-down-registry.png', 'http://localhost:8761');
  await capture('04-dependency-down-caller.png', endpoint);

  changedAt = Date.now();
  execFileSync('docker', ['unpause', mongo], { stdio: 'pipe' });
  await waitHealthy();
  await observe('dependency-restored', changedAt);
  await capture('05-dependency-restored-up.png', 'http://localhost:8761');

  changedAt = Date.now();
  await stop(business);
  await until('Graceful shutdown unregisters business', async () =>
    (await instances()).length === 0 && (await request(endpoint)).status === 503);
  await observe('graceful-shutdown', changedAt);
  await capture('06-graceful-shutdown.png', 'http://localhost:8761');

  business = startBusiness('business-health-disabled', 8082, false);
  await waitHealthy();
  changedAt = Date.now();
  execFileSync('docker', ['pause', mongo], { stdio: 'pipe' });
  await until('Actuator reports DOWN with health propagation disabled', async () =>
    (await request(health(8082))).body.status === 'DOWN');
  // Observe for several replication/renewal cycles rather than a single instant.
  for (let i = 0; i < 16; i++) {
    assert.ok(await hasStatus('UP'));
    await delay(1000);
  }
  const disabled = await observe('dependency-down-health-disabled', changedAt);
  assert.equal(disabled.health.status, 503);
  assert.equal(disabled.caller.status, 200);
  await capture('07-health-disabled-registry-up.png', 'http://localhost:8761');
  await capture('08-health-disabled-actuator-down.png', health(8082));
  execFileSync('docker', ['unpause', mongo], { stdio: 'pipe' });
  await waitHealthy();

  changedAt = Date.now();
  await stop(business, 'SIGKILL');
  await observe('abrupt-stop-immediate', changedAt);
  await capture('09-abrupt-stop-stale-registration.png', 'http://localhost:8761');
  await until('Abrupt failure evicted after lease/cache convergence', async () =>
    (await instances()).length === 0 && (await request(endpoint)).status === 503);
  await observe('abrupt-stop-evicted', changedAt);
  await capture('10-abrupt-stop-evicted.png', 'http://localhost:8761');

  changedAt = Date.now();
  startBusiness('business-restarted-8182', 8182);
  await waitHealthy(8182);
  const restarted = await observe('restarted-new-port', changedAt, 8182);
  assert.equal(restarted.caller.body.port, '8182');
  await capture('11-restarted-new-port.png', 'http://localhost:8761');
  await capture('12-restarted-caller-response.png', endpoint);
  evidence.completedAt = new Date().toISOString();
  await writeFile(new URL('results.json', output), JSON.stringify(evidence, null, 2) + '\n');
  console.log(`PASS: evidence saved to ${fileURLToPath(output)}`);
} catch (error) {
  evidence.error = String(error);
  await writeFile(new URL('failed-run.json', output), JSON.stringify(evidence, null, 2) + '\n');
  throw error;
} finally {
  if (browser) await browser.close();
  for (const child of processes.reverse()) await stop(child);
  if (mongoStarted) {
    try { execFileSync('docker', ['unpause', mongo], { stdio: 'pipe' }); } catch { }
    execFileSync('docker', ['stop', mongo], { stdio: 'pipe' });
  }
  if (databaseStarted) execFileSync('docker', ['stop', database], { stdio: 'pipe' });
}
