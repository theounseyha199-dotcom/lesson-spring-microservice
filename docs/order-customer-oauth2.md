# Order → Customer OAuth2

Order uses its own `order-service` OAuth client to call Customer. The authorization
server grants `customer.read` and `customer.write` because Order exposes both
customer lookup and creation. Tokens have audience `customer-service`.
Customer validates signature, expiry, issuer and audience, and requires:

- `customer.read` for GET `/api/customers` and its subpaths.
- `customer.write` for POST `/api/customers` and PUT `/api/customers/**`.

The health endpoint remains public for Consul. Other Customer endpoints are denied.
Spring's authorized-client manager caches Order's token and obtains another when
needed. No interactive user login is involved in this outbound call.

## Local startup

Set the same strong `ORDER_CLIENT_SECRET` environment variable in the authorization
server and Order run configurations. Generate a secret once, for example with
`openssl rand -hex 32`, and keep it outside source control. Customer does not need
this secret. The authorization server now requires it at startup.

Run `./run-all.sh` from the repository root. This starts the local IAM server
from `microservices/iam-service`, Customer (8084), Order (9092), the other services,
and their dependencies. The script generates a shared secret in
`.local/runtime.env`; source it when starting IAM or Order from an IDE.
Do not run a second authorization server on port 9000.

`AUTH_ISSUER` defaults to `http://localhost:9000` in both resource servers.
Order's `AUTH_TOKEN_URI` defaults to `http://localhost:9000/oauth2/token`.
Use HTTPS URLs and environment-specific secrets outside local development.
Consul must advertise a reachable Customer instance for `lb://customer-service`.

## Postman → Order

Use a **fresh** token after restarting the authorization server; old tokens do not
have the new audience. Configure OAuth 2.0 on `GET http://localhost:9092/customers`:

| Setting | Value |
| --- | --- |
| Grant type | Client Credentials |
| Token URL | `http://localhost:9000/oauth2/token` |
| Client ID | `postman-client` (existing local test client) |
| Client secret | Existing configured secret; local default `dev-secret-change-me` |
| Scope | `api.read` |
| Client authentication | Send as Basic Auth header |

Select Get New Access Token, Use Token, then Send. For POST through Order,
request `api.write` as well. Order enforces `api.read` on customer GET routes and
`api.write` on customer mutation routes. Its JWT audience is `order-service`.
Order then obtains a separate Customer token using its own credentials.
Do not use the Order service credential as a browser/frontend credential.
The existing payment client is separate and was not migrated in this change.

## Expected failures

- Missing, expired, incorrectly signed, wrong-issuer or wrong-audience token: 401.
- Valid token without the required endpoint scope: 403.
- Customer/token-server/discovery failures during Order's outbound request: Order's
  existing adapter reports 503; inspect Order logs for the underlying error.

A direct Customer test requires the `order-service` client credentials and
`customer.read` scope; a Postman `api.read` token is intentionally rejected there.

## Provisioning and rotation

Startup seeding is for development and only inserts clients that do not exist.
Changing `ORDER_CLIENT_SECRET` does **not** rotate a persisted client's secret.
For rotation, update the existing registered client's BCrypt secret through a
controlled administrative process, update Order's secret, then restart Order to
clear its cached client credentials/token. Previously issued access tokens remain
valid until expiry unless an additional revocation mechanism is used.
Use separate clients/secrets per environment and controlled provisioning in
production. User-specific data ownership checks remain a separate application
requirement; service tokens represent Order, not an end user.
