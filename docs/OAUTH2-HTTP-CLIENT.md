# OAuth2 HTTP service clients

The order service follows the [Spring Security HTTP Service Clients integration](https://docs.spring.io/spring-security/reference/features/integrations/rest/http-service-client.html), using RestClient for synchronous payment calls and WebClient for reactive customer calls.

- `CustomerHttpClient` uses `@ClientRegistrationId("order-service")`.
- `PaymentClient` uses `@ClientRegistrationId("payment-client")`.
- `ClientConfig` registers the `customer-service` and `payment` HTTP service groups.
- `OAuth2ClientConfig` binds standard `spring.security.oauth2.client` properties and creates synchronous and reactive application-scoped client-credentials managers.
- `OAuth2RestClientHttpServiceGroupConfigurer.from(manager)` installs the annotation processor and bearer-token interceptor for payment RestClient calls.
- `OAuth2WebClientHttpServiceGroupConfigurer.from(manager)` installs the corresponding OAuth2 filter for customer WebClient calls. Both managers cache tokens under the service identity and obtain new tokens when expired.

Registration IDs select credentials; group names select HTTP client configuration and base URLs. They do not need to be identical.

## Payment usage

`CheckoutServices.registerPayment` calls `PaymentClient.createPayment`. It sends
`POST /api/payments` with UUID order/customer IDs and an amount, matching the
existing payment controller. The controller does not expose a GET-by-order-ID endpoint.
The payment response remains part of the existing checkout response.

Base URLs in `application.yaml` use `lb://customer-service` and `lb://payment-service`
for service discovery. The `hardcoded-demo` profile overrides the payment URL to
`http://localhost:8083`.

## Your authorization server

The configuration matches `/home/seyha/web/micro-frontend/spring-authorization-service`:
`RegisteredClientConfig` seeds the machine client with `client_secret_basic`,
`client_credentials`, and `api.read`/`api.write`. Its `application.yml` defaults to
port 9000. The order service uses the root issuer's `/oauth2/token` endpoint.
No browser login or authorization-code redirect is needed for these service calls.

Start that project's PostgreSQL database and application first:

```bash
cd /home/seyha/web/micro-frontend/spring-authorization-service
docker compose up -d
./gradlew bootRun
```

The seed only inserts missing clients. If `AUTH_CLIENT_ID` or `AUTH_CLIENT_SECRET`
was changed, use the matching persisted credentials in the order service. Spring
property overrides can be used without editing YAML, for example
`SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_PAYMENTCLIENT_CLIENTSECRET`.

## Configure OAuth2

Edit the `oauth2` YAML document in
`microservices/order-service/order-service-main/src/main/resources/application.yaml`.
It uses plain values with no environment placeholders. Set the real token URL and
client credentials for each registration. Both use the local authorization server defaults: client ID `postman-client`,
development secret `dev-secret-change-me`, scopes `api.read,api.write`, and
`http://localhost:9000/oauth2/token`. These must match the client registered in
the authorization server database; environment overrides or existing records may differ.

The configured client must support `client_credentials` with `client_secret_basic`.
Add `scope` under a registration only for scopes supported by your authorization
server, such as a payment write scope for creating payments. Do not commit real secrets.

```bash
./gradlew -p microservices/order-service :order-service-main:bootRun \
  --args='--spring.profiles.active=dev,discovery-demo,oauth2'
```

Without `oauth2`, local calls remain unauthenticated. Enabling it requires a working
authorization server. This configures outbound credentials; downstream token validation
and authorization are separate. The remaining ad hoc customer/business checkout
lookups are not annotated HTTP service calls and do not receive OAuth2 tokens.

## Verify

```bash
./gradlew -p microservices/order-service :order-service-main:test \
  --tests '*OAuth2CustomerClientTest' \
  --tests '*CustomerDiscoveryControllerTest' \
  --tests '*BusinessDiscoveryTest'
```

Local HTTP servers verify standard property binding, each registration's credentials
and scope, bearer headers, payment POST payload, and token reuse. No live identity
provider is needed for these tests.
