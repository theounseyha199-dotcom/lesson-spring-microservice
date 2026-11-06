# Authorization Server infrastructure — Steps 7–8

## JDBC state

RegisteredClientRepository, OAuth2AuthorizationService and OAuth2AuthorizationConsentService are explicitly wired to their JDBC implementations. They use the existing oauth2_registered_client, oauth2_authorization and oauth2_authorization_consent tables. No migration SQL or domain/application framework boundary changed in this step.

JdbcAuthorizationPersistence configures the same Jackson 3 mapper for authorization reads and writes. It retains Spring Security's allowed types and additionally permits only the final IamUserPrincipal class and Long for OIDC updated_at timestamps. It does not allow arbitrary application packages or Object subtypes. Principal snapshots contain UUID, username, authority names and account flags; password credentials/hashes are excluded. Standard collection implementations keep stored token metadata compatible with the restricted mapper.

Integration tests reconstruct the JDBC services and reload authorization codes, redeemed-code metadata, access tokens, refresh tokens and consent. This verifies database-backed state without claiming that a full operating-system process restart was performed. Existing signing-key persistence supplies stable signature verification across application restarts.

## User claims

Local-user access and ID tokens use users.uuid as sub. Service-client subjects remain their client IDs. Access tokens contain fresh roles, permissions and authorities evaluated through GetUserAuthoritiesUseCase, including direct DENY, expiry and revocation. Issuance and refresh reject disabled, locked, deleted or otherwise unavailable users rather than trusting a stale session snapshot.

ID-token claims are limited to authorized scopes:

| Scope | Claims |
| --- | --- |
| openid | sub (internal user UUID) |
| profile | name, given_name, family_name, middle_name, nickname, preferred_username, profile, picture, website, gender, birthdate, zoneinfo, locale, updated_at |
| email | email, email_verified |
| phone | phone_number, phone_number_verified |
| address | address containing street_address, locality, region, postal_code, country |

Optional values that are not stored are omitted. Birthdate uses ISO local-date format; updated_at is epoch seconds. Verification flags use stored domain values. UserInfo uses the framework's scope-filtered ID-token projection and omits permission/role claims and authentication credentials. Access tokens carry authorization claims rather than copying personal profile claims. Existing order-service -> customer-service and api.read/api.write -> order-service audience behavior is preserved.

Fresh oidc-client seeds support all five OIDC scopes. Existing registered clients are preserved, including their original scope sets and secret hashes; startup does not silently widen or rotate them. Dynamically registered clients may request the supported OIDC scopes.

## Dynamic Client Registration

Set **IAM_REGISTRATION_CLIENT_SECRET** to a separate strong secret before starting IAM. This required environment variable backs app.iam.registration-client-secret. It is deliberately independent of provider credentials, the demo OIDC client secret and the Order client secret.

Startup creates client-registrar only if absent, with client_secret_basic, client_credentials and the single scope client.create. Repeated startup preserves its ID and secret hash. Changing the environment variable does not rotate an existing registered client's secret; provision the same value on subsequent starts.

1. Request an initial access token from POST /oauth2/token with HTTP Basic credentials client-registrar / IAM_REGISTRATION_CLIENT_SECRET, grant_type=client_credentials and scope=client.create.
2. POST /connect/register with that Bearer token and the assignment metadata:

```json
{
  "client_name": "ISTAD E-Commerce Web",
  "redirect_uris": ["http://localhost:8081/login/oauth2/code/iam"],
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "scope": "openid profile email",
  "token_endpoint_auth_method": "client_secret_basic"
}
```

3. The 201 response includes client_id, a generated plaintext client_secret, registration_access_token and registration_client_uri. The client secret is stored hashed and returned once. Native registration requires PKCE and user consent for the new client.
4. GET the returned registration_client_uri with its registration_access_token. The response omits client_secret rather than exposing its stored hash. A registration token cannot read a different client or create another registration.

The framework checks the token's persisted authorization and active state, requires exactly client.create for creation and client.read for configuration, and invalidates the initial token after one successful registration. Request a fresh initial token for each new client.

Spring Security 7.1.2 rejects nonempty DCR scopes by default. ClientRegistrationPolicy replaces only the scope policy while retaining default redirect/post-logout/JWKS validation. It allows openid/profile/email/phone/address with openid required; auth-code/refresh grants, code responses and client_secret_basic authentication are supported. Privileged client.create/client.read/service/permission scopes and client_credentials registration are rejected. Bearer-only endpoint failures return 401 rather than browser-login redirects.

## Verification and next work

Run ./gradlew check bootJar from iam-service. The Step 7 suite adds 19 protocol integration cases for scope inclusion/exclusion, full profile mapping, UserInfo privacy, JDBC reloads, consent, refresh permission updates/narrowing/rotation, disabled-account rejection, service compatibility, registrar idempotency, registration/read tokens, replay, scope escalation and metadata validation. Existing 44 REST/security cases and the domain/application/persistence/browser tests remain included.

Final Step 7 H2 result: 145 reported tests, 144 passed, one PostgreSQL-only container skipped, no failures/errors. Module-boundary verification and bootJar pass. PostgreSQL was not re-verified in this session; no live IAM database, provider secret, existing registered-client secret or signing key was changed.

## Step 8: provider callbacks and explicit account linking

Google/google, GitHub/github, Facebook/facebook and Telegram/telegram now enter through Spring Security's real
OAuth/OIDC callback filters. Code exchange and provider UserInfo loading run before the federation handler.
OIDC ID tokens retain signature, audience and nonce validation; the mapper also pins Google/Telegram issuers and
uses the signed subject. Google, GitHub and Telegram requests use S256 PKCE. Facebook retains OAuth state and its
server-side client-secret exchange. Facebook requests structured first_name/last_name/email fields; unavailable
attributes still require profile completion. Provider access/ID tokens, nonces and role/permission inputs are not
stored in the domain identity snapshot.

FederationLoginHandler calls FindOrProvisionFederatedUserUseCase after proof validation, then replaces the temporary
provider authentication with a credential-free IamUserPrincipal. Internal UUID subjects, current domain permissions,
JDBC authorization persistence and existing IAM token/REST behavior therefore apply to social users too. Provider
roles and matching emails never grant privileges or merge accounts. GitHub/Facebook email values are not treated as
verified; a Google/Telegram verification flag must be a boolean claim accompanying an email.

Missing required email/given/family names lead to GET/POST /federation/complete, not an authenticated IAM session.
The pending identity stays server-side for five minutes, is bound to an opaque flow ID and the session, and is
consumed after successful provisioning. Only missing fields can be completed; signed provider fields and verification
flags cannot be overwritten. Manually supplied email stays unverified. The form preserves values and exposes field
errors. CSRF, cancellation, expiry, replay protection and session-ID renewal apply. A new local sign-in invalidates a
pending completion when it is next accessed. Restarting the application loses an unfinished in-memory flow; restart
social sign-in in that case. Matching an existing email requires signing in to that account and explicitly linking.

Use **Manage connected accounts** on the account home, or GET /profile/accounts. A CSRF-protected
POST /profile/accounts/link/{provider} starts explicit linking. The owner UUID is stored with the OAuth authorization
request/state. The callback must return in the same authenticated internal-owner session; callback owner parameters
are never trusted. LinkIdentityUseCase retains provider ownership, historical uniqueness and relinking rules.
Successful linking preserves the internal session and returns to the accounts page. Provider cancellation or an
ownership conflict restores that same owner; an expired/changed owner cannot link. Identity unlinking rules
remain enforced by the domain use case; the Section F confirmation/unlink UI is implemented in Step 9.

Callback components and the security flow controller live in infrastructure; JTE views/static assets live in iam-web.
No inward framework dependency or adapter-to-adapter project dependency was added. Keep the browser login origin
consistent with the configured Facebook/Telegram ngrok callbacks so the OAuth state/session cookie returns.

Verification uses FederationIntegrationTests with a local HTTP provider, real callback filters, code exchanges,
UserInfo and RSA-signed ID tokens. It covers four providers, invalid signatures/nonces/audiences/issuers/tokens,
state/replay, profile expiry/completion/cancellation, privilege injection, verified-field immutability, email collisions,
linking CSRF/ownership/cancellation/conflicts/relinking, disabled users, and social-session IAM token issuance.
The final counts and build evidence are recorded in IMPLEMENTATION-STATUS.md. Live provider-account logins and a new
PostgreSQL run were not performed. Desktop/mobile browser previews use the actual test-rendered JTE output.

Protocol references: [Spring OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
and [Telegram OIDC](https://core.telegram.org/bots/telegram-login).

Step 9 now completes Section F pages/localization and production templates; see ../iam-web/README.md. Final delivery
verification remains. ConnectedApplications uses existing authorization/consent tables for owner-scoped app listing
and transactional revocation. The custom consent page posts back to the native Authorization Server endpoint.
