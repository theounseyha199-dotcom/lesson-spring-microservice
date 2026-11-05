# Local Consul customer lookup

Run commands from the repository root. This setup runs Java services on the host
and Consul and MongoDB in Docker Desktop. Consul health checks use
`host.docker.internal` to reach the host; the service addresses advertised by
Spring must also be reachable from the host for load-balanced requests.

Create `ite-net` with `docker network create ite-net` if it does not exist.

```bash
docker compose -f deployment/docker-compose.yaml up -d
docker compose -f deployment/consul/docker-compose.yaml up -d
```

Start each application in a separate terminal:

```bash
./gradlew -p microservices/customer-service :customer-service-main:bootRun
./gradlew -p microservices/order-service :order-service-main:bootRun
```

Defaults: customer-service uses port 8084, order-service uses 8081, and Consul
uses 8500. `CUSTOMER_PORT` and `ORDER_PORT` override the application ports and
their health-check ports.

Open http://localhost:8500 and wait for both service health checks to pass.

```bash
curl -s 'http://localhost:8500/v1/health/service/customer-service?passing=true'
curl -i -H 'Content-Type: application/json' \
  -d '{"username":"consul-demo","familyName":"Test","givenName":"Consul","email":"consul-demo@example.com","phoneNumber":"012345678"}' \
  http://localhost:8084/api/customers
```

Copy the returned customerId into this request:

```bash
curl -i http://localhost:8081/api/discovery/customers/CUSTOMER_UUID
```

Expected: HTTP 200 with customerId, username, and status. This endpoint invokes
CustomerServiceClientAdapter, whose load-balanced WebClient resolves
`http://customer-service` through Consul. An unknown UUID returns HTTP 404;
an unavailable customer service returns HTTP 503. Invalid UUID syntax returns 400.
The existing checkout flow remains separate from this demonstration endpoint.

If the healthy instance list is empty, inspect the check output in Consul and
ensure both databases and the customer application are running. On Docker Engine
without Docker Desktop, configure a container-reachable host address for the
health-check URL instead of assuming `host.docker.internal` resolves.

Automated client checks:

```bash
./gradlew -p microservices/order-service :order-service-client:test
```
