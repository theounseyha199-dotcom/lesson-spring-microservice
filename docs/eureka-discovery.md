# Eureka discovery and load balancing

The Part 1 baseline and screenshots are in
[the hard-coded communication report](part1-hardcoded-communication.md).

The discovery implementation adds a standalone Eureka server on **8761**.
Business, customer, order, and payment register under their `spring.application.name`.
They obtain the registry URL from `EUREKA_SERVER_URL`, defaulting to
`http://localhost:8761/eureka/`. This is a bootstrap registry address; business
instance addresses are discovered dynamically.

Spring Cloud **2025.1.2** is imported through a dependency BOM, compatible with
the existing Spring Boot 4.1.1 build according to
[Spring's compatibility table](https://spring.io/projects/spring-cloud/).
Clients share the `discovery-client-conventions` Gradle convention.

## Run and observe

Start the databases and build the services as described in
[the deployment guide](../deployment/README.md). Also build the registry:

```bash
./gradlew -p microservices/discovery-server test bootJar
java -jar microservices/discovery-server/build/libs/discovery-server-1.0.0.jar --spring.profiles.active=discovery-demo
```

Start each service jar in its own terminal with
`--spring.profiles.active=discovery-demo`. Omit `--eureka.client.enabled=false`
if following the Part 1 guide. To observe load balancing, start business twice:

```bash
java -jar microservices/business-service/business-service-main/build/libs/business-service-main-1.0.0.jar --spring.profiles.active=discovery-demo
java -jar microservices/business-service/business-service-main/build/libs/business-service-main-1.0.0.jar --spring.profiles.active=discovery-demo --server.port=8182
java -jar microservices/order-service/order-service-main/build/libs/order-service-main-1.0.0.jar --spring.profiles.active=discovery-demo
```

Visit `http://localhost:8761` and wait for both business instances to appear
as UP. Repeat:

```bash
curl -i http://localhost:8081/api/discovery/business
```

The returned `port` identifies the selected business instance. With a stable
two-instance list, successive calls use both ports through the default
round-robin load balancer. The order client uses `http://business-service` with
an `@LoadBalanced WebClient.Builder`; the service ID replaces an instance host
and port. The fixed-address Part 1 endpoint requires the `hardcoded-demo` profile.

The automated `BusinessDiscoveryTest` runs real HTTP servers on random ports
and supplies their locations through Spring's simple discovery client. It
checks that the production load-balanced controller reaches both instances
and returns HTTP 503 when both stop. This isolates load balancing from databases
and Eureka. A separate local check verified the actual Eureka server starting
and a real business instance registering UP. The full two-instance experiment,
failure convergence, and screenshots are recorded in
[the Part 3 report](part3-eureka-integration.md). Run `scripts/verify-part3.mjs`
to repeat the live integration test.

## Health monitoring and configuration

Each client includes Actuator and sets `eureka.client.healthcheck.enabled=true`
in `application.yaml`. Actuator aggregate health, including database health,
is propagated to Eureka. Heartbeats alone indicate that the client is renewing
its lease; they do not establish application dependency health. See
[Spring's Eureka health documentation](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html).

Only `health` and `info` management endpoints are exposed. Check
`/actuator/health` on each service and inspect its status in the registry.
Addresses advertised to Eureka must be reachable from callers. The configuration
prefers the host IP; in containers, use container-reachable registry and database
addresses and verify that the advertised IP is routable from the other services.
The instance ID includes service name, IP, and configured port, allowing two
local instances on different ports.

The default keeps Eureka's normal lease and cache settings and self-preservation.
The **discovery-demo** profile uses five-second renewals and client registry
fetches, a fifteen-second lease expiration, five-second server eviction/cache
intervals, and disables the additional LoadBalancer cache. The registry profile
also disables self-preservation solely to make local failure eviction observable.
Use this profile consistently on server and clients for the exercise.

Stop one business process gracefully and observe deregistration. A forced
termination requires lease expiry and server/client cache refresh; do not expect
instant removal. During that interval requests can still select the stale
instance and return 503. After convergence, requests should reach the survivor.
Stopping the business database can demonstrate dependency health propagation,
but it affects both local business instances because they share that database.

## Benefits and limitations

| Benefit | Remaining limitation |
| --- | --- |
| Callers use a stable service ID | A reachable registry bootstrap address is still required |
| New instances can register without caller source edits | Registration and registry caches converge asynchronously |
| LoadBalancer selects among available instances | Round-robin does not measure request cost or instance capacity |
| Health status can exclude unhealthy instances | Health and lease information can lag actual failures |
| Cached discovery data can survive short registry outages | New or changed instances cannot be learned while the registry is unavailable |

The three-second call deadline and HTTP 503 handling remain necessary. Discovery
does not provide a circuit breaker, guaranteed failover, safe write retries,
authentication, encryption, or distributed tracing. A production deployment
needs protected registry access and peer Eureka servers across failure domains;
the single local registry is a point of failure. Preserve self-preservation in
normal deployments because a network partition can otherwise trigger mass
eviction; see [Netflix's explanation](https://github.com/Netflix/eureka/wiki/Server-Self-Preservation-Mode).
