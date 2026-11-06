# Authorization Server infrastructure — Step 7

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

Next: Step 8 — wire validated Google/GitHub/Facebook/Telegram callbacks to internal provisioning and authenticated linking. The use cases already exist; provider callbacks currently do not perform that integration. Remaining JTE pages, English/Khmer localization, production template compilation and final delivery remain later work.
