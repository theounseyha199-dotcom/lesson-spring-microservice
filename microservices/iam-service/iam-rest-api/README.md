# IAM REST API — Step 6

The REST adapter implements all 39 method-route pairs in assignment Section D. Controllers map validated HTTP inputs to the existing application use cases. It depends only on iam-application-service and framework libraries; it does not access JPA, registered clients, or other adapters directly.

## Authentication and authorization

`POST /api/auth/register` is public. Every other `/api/**` request requires an IAM-issued JWT access token in `Authorization: Bearer <token>`. The API chain verifies the persistent IAM signing key, configured issuer, expiration and not-before time. Tokens without expiration, ID tokens and foreign-issuer tokens are rejected. Browser sessions and HTTP Basic do not authenticate API requests.

Permissions come from signed `authorities` / `permissions` claims as exact `resource:action` names. OAuth scopes and ROLE_ADMIN alone do not confer administrative permissions. The existing token customizer supplies authorities and now uses the internal user UUID for local-user access-token subjects; client-credentials subjects remain client IDs. Scope-controlled OIDC ID-token/UserInfo claims and JDBC authorization/consent persistence are implemented in Step 7; see ../iam-infrastructure/README.md.

| Routes | Authority |
| --- | --- |
| POST /api/users | user:create |
| GET /api/users, /api/users/{uuid}, /api/users/{uuid}/authorities, /api/users/{uuid}/identities | user:read |
| PATCH /api/users/{uuid}; POST enable, disable, lock, unlock; PUT password; DELETE identities/{identityUuid} | user:update |
| DELETE /api/users/{uuid} | user:delete |
| POST/DELETE user role and permission assignments | user:assign |
| POST /api/roles; GET role lists/details; PATCH and activate/deactivate; DELETE role | role:create, role:read, role:update, role:delete respectively |
| POST/DELETE role permission assignments | role:assign |
| POST /api/permissions; GET permission lists/details; PATCH and activate/deactivate; DELETE permission | permission:create, permission:read, permission:update, permission:delete respectively |
| GET/PATCH /api/me; PUT /api/me/password; GET /api/me/identities; DELETE /api/me/identities/{identityUuid} | authenticated internal user |

`/api/me` always takes its owner UUID from the verified token subject. Callers cannot choose an owner through JSON, query parameters or a browser session. Another owner's identity is not found in the current user's identity set (404). Registration accepts no roles, permissions, verified flags or account-state flags. Provider proof/linking is deliberately not accepted as an HTTP identity payload; provider callbacks belong to the federation step.

The API uses stateless bearer authentication with CSRF disabled only for `/api/**`. Authorization-server endpoints retain their own chain. Browser form login, OAuth login, sessions and browser CSRF protection are preserved.

## Inputs, responses and errors

- Registration/admin creation return 201. Reads, PATCH and assignment creation return 200. Password/account changes, deletion and revocation return 204.
- User, authority and identity projections match Section D.5. User details expose locale at the top level; raw profile/authentication objects, hashes and provider attributes are not serialized. Lists use `content` and `page` metadata.
- PATCH distinguishes omitted properties from explicit null. Optional fields can be cleared; required fields cannot. Unknown properties are rejected, including immutable/privileged fields.
- New passwords follow Step 5 policy: minimum 12 characters and maximum 72 UTF-8 bytes. Confirmation and current-password checks are delegated to existing use cases. The assignment's shorter sample change-password value must be lengthened to satisfy that policy.
- Assignment inputs accept up to 100 distinct UUIDs, optional future expiresAt and reason. Revocation DELETE requests require a JSON body with a nonblank reason, e.g. `{"reason":"Temporary access ended"}`. Identity unlink and ordinary entity deletion need no body.
- Lists accept page (default 0), size (default 20, maximum 100), sort (default createdAt,desc), and search. Users additionally accept enabled/locked; roles accept active; permissions accept resource/action/active. Sort fields are whitelisted by the application query types.
- Validation failures use RFC 9457 application/problem+json with type `https://iam.istad.co/problems/validation-error` and an `errors` array of field/message objects. Parser errors never echo submitted passwords. Missing resources return 404; uniqueness, protected-system-record, assignment and concurrency conflicts return 409. Missing/invalid tokens return JSON 401 with WWW-Authenticate; insufficient permission returns JSON 403.

## Verification

Run from iam-service:

```bash
./gradlew check bootJar
```

RestApiIntegrationTests uses real signed RSA tokens, the production filter chains, MockMvc and database-backed use cases. It covers all administrative routes with missing/wrong/exact permissions, all 39 routes in successful workflows, registration privilege rejection, validation, PATCH null handling, paging, password changes, account flags, assignment/revocation, DENY precedence, identity ownership, safe output, token tampering/expiry/issuer, browser session isolation and a real Authorization Code + PKCE token exchange followed by `/api/me`.

Default tests use H2 and temporary signing keys. PostgreSQL tests require the isolated iam_test_... database described in ../iam-persistence/README.md. No tests should point at db_oauth2.
