# IAM service

The repository's authorization server uses PostgreSQL on port 9543 (`db_oauth2`).
Run the full project with `./run-all.sh` at the repository root; see
[the run guide](../../README.md).

Login: `http://localhost:9000/login`, local user `user` / `password`.
OIDC client: `oidc-client` / `secret`, Authorization Code with PKCE (SHA-256),
scopes `openid profile`. Postman callback:
`https://oauth.pstmn.io/v1/browser-callback`.

The startup runner inserts missing OIDC, Order and Postman clients after database
migrations. Existing client IDs and secrets are preserved. Set the same
`ORDER_CLIENT_SECRET` for IAM and Order (the startup script generates it once).
Order tokens target Customer; Postman API tokens target Order.

The signing key persists at `IAM_SIGNING_KEY_FILE`, default
`.local/iam-signing-key.json`, with owner-only permissions on Linux. Back it up
and keep it outside Git. The startup script uses an absolute path.

Database overrides: `IAM_DB_URL`, `IAM_DB_USER`, `IAM_DB_PASSWORD`.
Server overrides: `IAM_PORT`, `IAM_ISSUER`. The OIDC secret can be supplied with
`app.iam.client-secret` (`APP_IAM_CLIENTSECRET` using Spring's environment binding).
Changing a secret property does not rotate an existing registered client.

Tests use H2 and temporary signing keys. The test changelog adapts PostgreSQL's
multiple ADD COLUMN syntax for H2; production migrations remain unchanged.

For direct IntelliJ runs, IAM and Order also load the ignored
`.local/runtime.properties` file when the working directory is the repository
root or the service directory. Put `ORDER_CLIENT_SECRET=<shared secret>` in this
file, using the same value as `.local/runtime.env` and the existing registered
Order client. Environment variables override this file. Keep its permissions
restricted (`chmod 600 .local/runtime.properties`).

## Facebook login

1. Create an app at [Meta for Developers](https://developers.facebook.com/apps/)
   and configure Facebook Login for a website.
2. Copy the App ID and App Secret into `client-id` and `client-secret` under
   `spring.security.oauth2.client.registration.facebook` in
   `iam-main/src/main/resources/application.yaml`, replacing the placeholder strings.
3. Enable Web OAuth Login and register this exact Valid OAuth Redirect URI:
   `https://lagging-grouped-bobsled.ngrok-free.dev/login/oauth2/code/facebook`.
4. Restart IAM, keep the ngrok tunnel to port 9000 running, and open
   `https://lagging-grouped-bobsled.ngrok-free.dev/login`. Choose
   **Continue with Facebook**. Start on the same host as the callback to preserve
   the session containing the OAuth state.

The authorization start route is `/oauth2/authorization/facebook`; the callback
route is `/login/oauth2/code/facebook`. If the tunnel hostname changes, update
both YAML and Meta's registered redirect URI. The server requests only `public_profile` and identifies the account by Facebook's
`id`. Email permission is not requested. External accounts are not automatically linked to local users
or assigned local roles. Facebook endpoints use the app's default Graph API
version. The secret is sent only during the server-side token exchange.

Live sign-in requires real Meta credentials and the app's login permissions and
availability to be configured for the intended users.

## Module structure (Step 2)

| Module | Responsibility |
| --- | --- |
| iam-domain/iam-domain-core | Immutable domain models, audited assignments and Section B rules |
| iam-domain/iam-application-service | Application contracts and output ports |
| iam-persistence | Complete JPA mappings, repository adapters, audited assignments and Liquibase upgrades |
| iam-infrastructure | Security, Authorization Server, registered-client initialization and signing keys |
| iam-rest-api | 39 secured REST routes, validated DTOs and RFC 9457 errors |
| iam-web | Browser controllers, JTE templates, static assets and view configuration |
| iam-main | Boot entry point, application configuration, domain-based seeding and integration tests |

From this directory run `./gradlew test bootJar` or `./gradlew bootRun`.
From `microservices/`, existing `./gradlew :iam-service:test :iam-service:bootJar` commands remain available.
The executable artifact now lives in `iam-main/build/libs/`.
Reload Gradle in IntelliJ and select the **iam-main** module for `IamServiceApplication`.
The Java main-class name is unchanged. Development templates are resolved from iam-web
when launched from the module, service, microservices or repository directory.

`./gradlew verifyModuleBoundaries` checks adapter dependency direction and rejects framework
imports in domain/application sources. The adapters depend on application contracts, not one another.
JPA models use `istad.ha.iamservice.persistence.entity`; repositories and adapters use
`persistence.repository` and `persistence.adapter`. Security uses `infrastructure.config`,
client initialization uses `infrastructure.init`, browser code uses `web.controller`/`web.config`,
and application seeding uses `bootstrap`. Java package declarations match their source paths.
The empty legacy service-root `src` tree has been removed. Migration SQL contents and classpath resource paths
are unchanged. No database reset is required for the module move.

JTE still uses development compilation. Building bootJar verifies packaging, not standalone JTE
rendering inside a packaged jar; production template precompilation remains a later task.

## Domain model (Step 3)

The [domain guide](iam-domain/iam-domain-core/README.md) explains the complete field mapping,
authority and identity rules, and persistence contracts. Run the isolated rules with
`./gradlew :iam-domain:iam-domain-core:test` from this directory. New domain records remain
independent of Spring/JPA and are mapped through the persistence adapters.

## Persistence upgrade (Step 4)

The [persistence guide](iam-persistence/README.md) covers schema changes, preservation checks,
versioning, identity locking and PostgreSQL test setup. Existing SQL migrations are unchanged;
new changesets upgrade the schema when IAM next starts. Stop the old application before running
the new version because several legacy columns are renamed. The live database has not been
migrated during implementation. Default tests remain isolated in H2; PostgreSQL verification
uses a separate iam_test_... database.

## Application use cases (Step 5)

The [application guide](iam-domain/iam-application-service/README.md) lists all 39 Section C use cases,
commands/results, transaction behavior, paging, password policy and federation trust boundaries.
Local authentication calls the application use case and records login attempts. REST endpoints are implemented in Step 6; provider-callback provisioning remains a later step.

## Secured REST APIs (Step 6)

All 39 Section D routes are implemented. See the [REST adapter guide](iam-rest-api/README.md) for routes,
permissions, input/output contracts and test coverage. POST /api/auth/register is public; other API routes
require an own-issuer JWT access token. Administrative operations enforce exact permission authorities;
/api/me derives ownership from the internal UUID subject. Browser sessions and CSRF remain separate.

Local-user access tokens now use the internal UUID subject; service-client subjects are preserved.
Scope-controlled OIDC claims, JDBC authorization/consent persistence and dynamic client registration are implemented in Step 7. Production template precompilation and later browser/federation features remain pending.

## Authorization Server features (Step 7)

JDBC authorization/consent persistence, scope-controlled OIDC claims, UUID user subjects and Dynamic Client
Registration are complete. See the [infrastructure guide](iam-infrastructure/README.md) for claims, registration,
security policy and verification details.

Set `IAM_REGISTRATION_CLIENT_SECRET` to a separate strong secret before starting IAM. Startup seeds
`client-registrar` with only `client.create`; existing client secrets and scopes are preserved. Initial registration
tokens are single-use. POST `/connect/register` returns a generated secret once and a registration token for reading
that specific client. Native registration requires PKCE/consent and accepts only the supported OIDC scopes.

Step 7 verification: 144 passed, one PostgreSQL-only container skipped (145 reported), zero failures/errors;
`check`, module boundaries and `bootJar` pass. The next step is provider callback provisioning/linking (Step 8).
