# IAM service

Spring Authorization Server / OIDC IAM with a plain Java domain, 39 assignment use cases, PostgreSQL/Liquibase
persistence, 39 secured REST routes, Dynamic Client Registration, four social providers and English/Khmer JTE pages.
Implementation Steps 1–9 and the automated Step 10 delivery audit are complete. Live provider-account acceptance
still requires a human sign-in; see [the acceptance checklist](LIVE-PROVIDER-ACCEPTANCE.md).

## Run locally

Java 17 and PostgreSQL are required. The existing local PostgreSQL service exposes port 9543; the default application
database is db_oauth2. Build and start from this directory:

```sh
./gradlew check bootJar
java -jar iam-main/build/libs/iam-main-0.0.1-SNAPSHOT.jar
```

Open http://localhost:9000/login. The default now uses packaged precompiled templates, so no template sources are
needed at runtime. Activate the dev profile only for live template editing. Reload the IntelliJ Gradle model after
adding the JTE plugin. Existing migrations 001–005 are preserved; migration 006 upgrades the legacy domain schema.
Stop an old binary before starting the new schema version because some legacy columns were renamed.

## Required external configuration

All IAM provider, database, bootstrap and client passwords/secrets are supplied through environment variables.
For workstation convenience, application.yaml also imports the Git-ignored repository .local/runtime.properties.
For a **new** checkout, use [runtime.properties.example](runtime.properties.example) as a key-only template there.
If a local file already exists, merge missing keys; do not overwrite its current values. This workstation's current
values were preserved locally during the audit. Never commit a populated runtime file or Postman environment.

| Purpose | Variables |
| --- | --- |
| PostgreSQL | IAM_DB_URL (default localhost:9543/db_oauth2), IAM_DB_USER (default iteuser), IAM_DB_PASSWORD |
| Local bootstrap users | IAM_BOOTSTRAP_ADMIN_PASSWORD, IAM_PASSWORD, IAM_BOOTSTRAP_OWNER_PASSWORD |
| OIDC client | IAM_CLIENT_ID (default oidc-client), IAM_CLIENT_SECRET |
| Service clients | ORDER_CLIENT_SECRET, IAM_POSTMAN_CLIENT_SECRET |
| Registration client | IAM_REGISTRATION_CLIENT_SECRET |
| Social providers | IAM_GOOGLE_CLIENT_ID/SECRET, IAM_GITHUB_CLIENT_ID/SECRET, IAM_FACEBOOK_CLIENT_ID/SECRET, IAM_TELEGRAM_CLIENT_ID/SECRET |
| Issuer/signing | IAM_ISSUER, IAM_PORT, IAM_SIGNING_KEY_FILE (optional; existing defaults retained) |
| Fresh OIDC callbacks | IAM_REDIRECT_URI, IAM_POSTMAN_REDIRECT_URI, IAM_LOGOUT_REDIRECT_URI (optional) |

Use strong unique values for new deployments. Bootstrap inserts only missing users, roles and clients; changing
configuration does **not** rotate stored passwords/client secrets or silently expand existing scopes. Current weak
historical demo passwords are not reset by this change. The OIDC client ID/callback settings now apply to fresh seeds.
Existing clients must be managed separately. An older OIDC client may have only openid/profile; request its existing
scopes or register a new client with the needed supported scopes rather than silently modifying it.

Register each exact callback URL in its provider console. Google/GitHub default to the current base URL. The repository's
Facebook/Telegram settings currently name the existing ngrok origin; override their Spring redirect-uri properties for
your deployment and start sign-in on that same origin so OAuth state/session cookies return. Provider IDs/credentials
cannot be validated solely by loading the configuration. No provider credentials were rotated during this audit.

The persistent signing key defaults to .local/iam-signing-key.json. Keep the same protected key across application
restarts and instances as appropriate; removing it creates a new signature identity and invalidates old token checks.

## Architecture and contracts

| Module | Responsibility / project dependencies |
| --- | --- |
| iam-domain-core | Domain records/rules; JDK only |
| iam-application-service | Assignment use cases and output ports; domain-core |
| iam-persistence | JPA/Liquibase adapters; application-service |
| iam-infrastructure | Security, federation, OAuth/JDBC and transaction adapters; application-service |
| iam-rest-api | Strict DTOs, permission/ownership checks and Problem Details; application-service |
| iam-web | JTE controllers, shared templates and localization; application-service |
| iam-main | Entry point/bootstrap and composition of all modules |

The original 39 use cases plus three browser-support use cases are present. Adapters do not depend on one another.
`./gradlew verifyModuleBoundaries` checks dependencies and framework-free inner sources. The Authorization Server
is an adapter rather than the domain model. Current UUID subjects and domain permissions govern user access tokens;
service-client subjects and existing audiences remain separate.

- [Domain guide](iam-domain/iam-domain-core/README.md): field validation, effective permissions and identity history.
- [Application guide](iam-domain/iam-application-service/README.md): use cases, typed inputs, trust/transaction boundaries.
- [Persistence guide](iam-persistence/README.md): forward migrations, uniqueness, audit/versioning and disposable tests.
- [REST guide](iam-rest-api/README.md): all 39 routes, exact authorities, safe responses and RFC 9457 errors.
- [Authorization/federation guide](iam-infrastructure/README.md): JDBC state, scoped claims, DCR and callback proofs.
- [Browser guide](iam-web/README.md): all eight Section F pages, confirmation flows, localization and packaged templates.

Registration assigns USER only. Administrative routes require exact resource:action permissions; a role name or
OAuth scope alone is insufficient. /api/me derives ownership from an IAM-issued user access-token subject. Provider
claims never grant internal roles, matching emails do not auto-merge accounts, and a missing mandatory provider
profile requires an expiring completion flow. Historical identity ownership and the last sign-in method are protected.
App revocation removes the owner's persisted grants/consent and prevents refresh, but already issued self-contained
JWTs can remain valid at downstream resource servers until expiry.

## Postman and verification

Use [IAM-Assignment.postman_collection.json](../../postman/IAM-Assignment.postman_collection.json) and its empty-secret
[environment](../../postman/IAM-Assignment.postman_environment.json). Follow the [Postman guide](../../postman/README.md).
Use Authorization Code + SHA-256 PKCE with a real internal user; use a permission-bearing admin user for admin routes.
The collection includes all 39 REST routes, OAuth/OIDC metadata and DCR. Identity/password/cleanup operations require
explicit setup and should be run individually against disposable data.

```sh
./gradlew check bootJar
python3 scripts/audit-delivery.py
```

Final verification on 6 November 2026:

| Check | Result |
| --- | --- |
| H2 suite | 182 reported: 181 passed, 1 PostgreSQL-only container skipped; 0 failures/errors |
| Disposable PostgreSQL 18.4 suite | 184 passed, 0 failures/errors/skips; temporary database removed |
| Gradle / packaging | check, verifyModuleBoundaries and bootJar pass |
| Postman runtime | 44 requests, 66 assertions, 0 failures against isolated packaged-app data |
| Static delivery contract | 9 checks pass: 39 routes, 39 required use cases, locales/pages, secrets and legacy migrations |
| Browser verification | Step 9: 48 Chromium workflow/layout checks pass; packaged JAR runs without template sources |

The Postman run excludes the interactive OAuth-helper folder and manual password/identity actions. Those REST routes
remain covered by the server integration tests. For PostgreSQL testing, provide IAM_TEST_DB_URL with a **disposable**
database name matching iam_test_..., IAM_TEST_DB_USER and IAM_TEST_DB_PASSWORD. The test harness rejects other names.
Never point tests at db_oauth2.

See [DELIVERY-AUDIT.md](DELIVERY-AUDIT.md) for the final evidence and outstanding human acceptance. No live application
database reset, credential rotation, deployment, commit or push was performed by the delivery audit.


## Live-provider readiness follow-up

The current ngrok HTTPS origin is reachable. Forwarded-header handling now preserves HTTPS callback URLs, with an
embedded-server regression and local IAM restart verified. GitHub/Facebook/Telegram reach their real sign-in pages.
Google reaches sign-in from localhost; its public flow is blocked until the exact HTTPS callback is added to the
Google OAuth client's authorized redirect URIs. See [live results](LIVE-PROVIDER-RESULTS.md). Provider account login/MFA
remains manual; authorization-page checks do not claim an end-to-end login.
