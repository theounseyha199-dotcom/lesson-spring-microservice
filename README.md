# Spring microservices project

Gradle composite builds for IAM, Order, Customer, Business, Payment and Gateway, with shared libraries and local
infrastructure definitions. Use Java 17. See [local dependencies](deployment/README.md) and [Postman usage](postman/README.md).

## IAM service

The IAM service provides local and social login, OAuth/OIDC tokens, Dynamic Client Registration, permission-based REST
APIs and English/Khmer account pages. It defaults to http://localhost:9000 and PostgreSQL localhost:9543/db_oauth2.

From the repository root:

```sh
./gradlew -p microservices/iam-service check bootJar
java -jar microservices/iam-service/iam-main/build/libs/iam-main-0.0.1-SNAPSHOT.jar
```

Supply secrets through environment variables. For local development, IAM also reads the Git-ignored
.local/runtime.properties. Use [the key-only template](microservices/iam-service/runtime.properties.example) for a new
checkout; merge missing keys if a local file already exists. Never commit a populated runtime file or Postman environment.

| Configuration | Variables |
| --- | --- |
| PostgreSQL | IAM_DB_URL, IAM_DB_USER, IAM_DB_PASSWORD |
| Bootstrap passwords | IAM_BOOTSTRAP_ADMIN_PASSWORD, IAM_PASSWORD, IAM_BOOTSTRAP_OWNER_PASSWORD |
| OAuth client secrets | IAM_CLIENT_SECRET, ORDER_CLIENT_SECRET, IAM_POSTMAN_CLIENT_SECRET, IAM_REGISTRATION_CLIENT_SECRET |
| Provider credentials | IAM_GOOGLE_CLIENT_ID/SECRET, IAM_GITHUB_CLIENT_ID/SECRET, IAM_FACEBOOK_CLIENT_ID/SECRET, IAM_TELEGRAM_CLIENT_ID/SECRET |
| Optional issuer/signing settings | IAM_ISSUER, IAM_PORT, IAM_SIGNING_KEY_FILE |
| Optional fresh-client settings | IAM_CLIENT_ID, IAM_REDIRECT_URI, IAM_POSTMAN_REDIRECT_URI, IAM_LOGOUT_REDIRECT_URI |

Bootstrap inserts missing records and preserves existing users/passwords and registered clients. Changing seed settings
does not rotate stored secrets or expand existing scopes. New user passwords require at least 12 characters and at most
72 UTF-8 bytes. Default production mode uses packaged precompiled JTE templates; activate the dev profile for live editing.

### Modules

| Module | Responsibility |
| --- | --- |
| iam-domain-core | Framework-free domain models and business rules |
| iam-application-service | Use cases and output ports; depends on domain-core |
| iam-persistence | JPA and Liquibase adapters |
| iam-infrastructure | Security, OAuth, federation and transaction adapters |
| iam-rest-api | Strict DTOs, authorization and Problem Details |
| iam-web | JTE pages, forms and localization |
| iam-main | Application entry point, bootstrap and composition |

Adapters depend on the application layer, not one another. `verifyModuleBoundaries` checks this direction. The REST API
contains 39 routes under /api/auth, /api/users, /api/roles, /api/permissions and /api/me. Public registration assigns USER
only; admin routes require exact resource:action permissions. /api/me takes ownership from an IAM-issued user access token.
Browser sessions, ID tokens and client-credentials tokens cannot substitute for a user access token at that API.

### Browser routes

| Route | Purpose |
| --- | --- |
| /login, /register | Local/social sign-in and registration |
| /oauth2/consent | Client name/scopes and allow/cancel |
| / | Profile claims and current roles/permissions |
| /profile/edit, /profile/password | Profile and password changes |
| /profile/accounts | Provider linking and disconnection |
| /profile/apps | Authorized applications and access revocation |

Switch language with ?lang=en or ?lang=km. Profile saves reject stale versions. Destructive account/app actions require
confirmation and browser CSRF protection. App revocation removes stored authorizations/consent and prevents refresh;
already issued JWTs may remain valid downstream until expiry.

### Social login

Google, GitHub, Facebook and Telegram callbacks validate provider proof before provisioning an internal user. Provider
roles are ignored and matching emails never auto-merge accounts. Missing mandatory profile data requires completion;
manual email entry is unverified. Identity history and the last sign-in method are protected.

Register each exact callback in the provider console. For the existing public tunnel, callbacks are
https://lagging-grouped-bobsled.ngrok-free.dev/login/oauth2/code/{google|github|facebook|telegram}.
Start sign-in from the same origin as its callback so OAuth state/session cookies return. Native trusted-proxy handling
preserves HTTPS behind ngrok; deployments must configure their actual trusted proxies.

Google's localhost sign-in page is reachable. Its public callback currently needs to be added to the Google Web OAuth
client's Authorized redirect URIs. GitHub/Facebook/Telegram reach their real sign-in pages. Provider account/MFA sign-in
must still be completed to verify the full flow. These readiness checks are not a claim of completed live-account acceptance.

### Tests and API requests

```sh
./gradlew -p microservices/iam-service check bootJar
python3 microservices/iam-service/scripts/audit-delivery.py
```

The latest checks passed: H2 181 passed/1 PostgreSQL-only container skipped (182 reported); PostgreSQL 184 passed with no
failures or skips. PostgreSQL tests require IAM_TEST_DB_URL pointing to a disposable database named iam_test_..., plus
IAM_TEST_DB_USER and IAM_TEST_DB_PASSWORD. Never point tests at db_oauth2. Original migrations 001–005 are preserved;
forward migration 006 upgrades the legacy schema, so stop an old binary before running the new schema version.

Import [the IAM collection](postman/IAM-Assignment.postman_collection.json) and [its empty-secret environment](postman/IAM-Assignment.postman_environment.json).
Use Authorization Code + S256 PKCE with an internal user; admin requests need a permission-bearing admin account.
Collection verification passed 44 requests/66 assertions against isolated data. Password, identity and cleanup requests
need explicit target values and should be run individually. The other microservices have their separate collection;
the IAM checks do not claim a fresh end-to-end run of every service.
