// Local integration: JDK 25, mongod, PostgreSQL binaries, Playwright/Chromium.
// Owns isolated database directories and child processes; does not touch development databases.
import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { openSync, closeSync } from 'node:fs';
import { mkdir, mkdtemp, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const out = root + 'docs/screenshots/checkout/';
await mkdir(out, { recursive: true });
const tmp = await mkdtemp('/tmp/eureka-checkout-');
const children = [];
const evidence = { startedAt: new Date().toISOString(), requests: [], checks: [] };
const delay = ms => new Promise(r => setTimeout(r, ms));
let browser;
function start(name, command, args) {
  const fd = openSync(out + name + '.log', 'w');
  const c = spawn(command, args, { cwd: root, stdio: ['ignore', fd, fd] });
  closeSync(fd); children.push(c); return c;
}
async function stop(c) {
  if (c.exitCode !== null || c.signalCode !== null) return;
  const done = new Promise(r => c.once('exit', r)); c.kill('SIGTERM');
  await Promise.race([done, delay(10000)]);
  if (c.exitCode === null && c.signalCode === null) { c.kill('SIGKILL'); await done; }
}
async function req(url, body, method = body ? 'POST' : 'GET') {
  const r = await fetch(url, { method, headers: {'Content-Type':'application/json', Accept:'application/json'},
    ...(body ? {body: JSON.stringify(body)} : {}), signal: AbortSignal.timeout(15000) });
  const text = await r.text(); let data; try { data = JSON.parse(text); } catch { data = text; }
  return { url, method, status: r.status, body: data };
}
async function until(label, fn) {
  const end = Date.now() + 120000; let error;
  while (Date.now() < end) { try { if (await fn()) { console.log(label); return; } } catch(e) {error=e;}
    await delay(1000); }
  throw Error(label + ': timeout ' + (error || ''));
}
function app(service, port, extra = [], baseline = false) {
  const jar = service === 'discovery-server' ? `microservices/${service}/build/libs/${service}-1.0.0.jar`
    : `microservices/${service}/${service}-main/build/libs/${service}-main-1.0.0.jar`;
  return start(service + '-' + port, process.env.JAVA_COMMAND || 'java', ['-jar',jar,
    '--spring.profiles.active=' + (baseline ? 'hardcoded-demo' : 'discovery-demo'),
    '--server.port=' + port, '--eureka.instance.ip-address=127.0.0.1',
    '--spring.mongodb.uri=mongodb://localhost:12027/checkout_' + service.replaceAll('-','_'),
    '--spring.datasource.url=jdbc:postgresql://localhost:15434/istad_order',
    '--spring.datasource.username=istad','--spring.datasource.password=istad', ...extra]);
}
async function ready(port) { await until('Healthy '+port, async()=> (await req(`http://localhost:${port}/actuator/health`)).body.status === 'UP'); }
try {
  execFileSync('initdb', ['-D', tmp+'/pg', '-U','istad','-A','trust'], {stdio:'pipe'});
  start('postgres','postgres',['-D',tmp+'/pg','-p','15434','-h','127.0.0.1','-k',tmp]);
  await mkdir(tmp+'/mongo');
  start('mongo','mongod',['--dbpath',tmp+'/mongo','--port','12027','--bind_ip','127.0.0.1']);
  await until('PostgreSQL ready', async()=> { try { execFileSync('pg_isready',['-h','127.0.0.1','-p','15434'],{stdio:'pipe'}); return true; } catch{return false;} });
  execFileSync('createdb',['-h','127.0.0.1','-p','15434','-U','istad','istad_order']);
  app('discovery-server',8761); await ready(8761);
  for (const [name,port] of [['customer-service',8084],['business-service',8082],['payment-service',8083]]) app(name,port);
  for (const port of [8084,8082,8083]) await ready(port);
  let order = app('order-service',8081,[],true); await ready(8081);
  const customer = await req('http://localhost:8084/api/customers', {username:'eureka-demo',familyName:'Sok',givenName:'Dara',email:'dara@example.com',phoneNumber:'012345678'});
  const business = await req('http://localhost:8082/api/businesses', {name:'Discovery Market',email:'shop@example.com',address:'Phnom Penh'});
  assert.equal(customer.status,201); assert.equal(business.status,201);
  evidence.requests.push(customer,business);
  const payload = { customerId:customer.body.customerId,businessId:business.body.businessId,deliveryAddress:'Phnom Penh',items:[{productId:'33333333-3333-3333-3333-333333333333',quantity:2,unitPrice:5}] };
  const baseline = await req('http://localhost:8081/api/checkout',payload);
  assert.equal(baseline.status,201); assert.equal(baseline.body.payment.amount,10);
  evidence.requests.push({phase:'hardcoded-checkout',...baseline});
  await stop(order); order=app('order-service',8081); await ready(8081);
  await until('All four service IDs registered UP',async()=> {
    const r=await req('http://localhost:8761/eureka/apps');
    const apps=r.body.applications.application || [];
    return ['CUSTOMER-SERVICE','BUSINESS-SERVICE','ORDER-SERVICE','PAYMENT-SERVICE'].every(n=>apps.some(a=>a.name===n && a.instance.some(i=>i.status==='UP')));
  });
  await until('Order discovery cache ready',async()=> (await req('http://localhost:8081/api/discovery/business')).status===200);
  // Allow customer/payment entries from the same registry snapshot to become available.
  const checkout=await req('http://localhost:8081/api/checkout',payload);
  assert.equal(checkout.status,201); assert.equal(checkout.body.payment.amount,checkout.body.order.total);
  assert.equal(checkout.body.payment.orderId,checkout.body.order.orderId);
  assert.equal(checkout.body.payment.status,'PENDING');
  evidence.requests.push({phase:'discovered-checkout',...checkout});
  for(const [label,body,status] of [
    ['missing-customer',{...payload,customerId:'00000000-0000-0000-0000-000000000000'},404],
    ['missing-business',{...payload,businessId:'00000000-0000-0000-0000-000000000000'},404],
    ['invalid-item',{...payload,items:[null]},400]]) {
    const r=await req('http://localhost:8081/api/checkout',body); assert.equal(r.status,status); evidence.requests.push({phase:label,...r});
  }
  const {chromium}=await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
  browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
  const page=await browser.newPage({viewport:{width:1280,height:1000}});
  await page.goto('http://localhost:8761'); await page.screenshot({path:out+'01-four-services-up.png',fullPage:true});
  // Browser-rendered transcript of the actual POST responses, labeled with URL and status.
  await page.setContent('<html><body><h1>Checkout HTTP evidence</h1><pre></pre></body></html>');
  await page.locator('pre').evaluate((el,data)=>el.textContent=JSON.stringify(data,null,2),evidence.requests.filter(r=>r.phase?.includes('checkout')));
  await page.screenshot({path:out+'02-checkout-responses.png',fullPage:true});
  const payment=children.find(c=>c.spawnargs.some(a=>a.includes('payment-service-main-1.0.0.jar')));
  await stop(payment);
  const partial=await req('http://localhost:8081/api/checkout',payload);
  assert.equal(partial.status,202); assert.equal(partial.body.outcome,'PAYMENT_RECONCILIATION_REQUIRED');
  assert.ok(partial.body.order.orderId); assert.equal(partial.body.payment,null);
  evidence.requests.push({phase:'payment-unavailable',...partial});
  await req('http://localhost:8084/api/customers/'+payload.customerId+'/deactivate',null,'PUT');
  const inactive=await req('http://localhost:8081/api/checkout',payload); assert.equal(inactive.status,409);
  evidence.requests.push({phase:'inactive-customer',...inactive});
  evidence.completedAt=new Date().toISOString();
  await writeFile(out+'results.json',JSON.stringify(evidence,null,2)+'\n');
  console.log('PASS: hard-coded and discovered checkout, validation and partial failure');
} finally {
  if(browser) await browser.close();
  for(const c of children.reverse()) await stop(c);
}
