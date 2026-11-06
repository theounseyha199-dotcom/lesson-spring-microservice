# IAM implementation status

## Phase 1: baseline audit — complete

Reviewed 6 November 2026 against ASSIGNMENT-CHECKLIST.md and all 14 assignment pages.
This report records inspected source and executed checks; it is not a score estimate.

| Requirement | Baseline | Evidence / remaining work |
| --- | --- | --- |
| A: seven modules and dependency boundaries | Missing | settings.gradle declares only iam-service; boot, domain, persistence and web share src/main. |
| B.1: complete User model | Partial | domain/User.java has basic profile/account flags and user auditing. UUID is String/VARCHAR; birth date is LocalDateTime; required field names/types/constraints differ. Missing remaining claims, failed attempts, password/login timestamps, soft deletion and version. |
| B.2–B.3: roles and permissions | Partial | Existing JPA models lack UUIDs, system/active flags, complete audit/deletion/version metadata and resource/action structure. |
| B.4: identities | Missing | No Identity model or persistent social-account association. |
| B.5: audited assignments | Missing | User and Role use @ManyToMany. Existing users_roles/roles_permissions tables contain only keys and cascading deletes. Required singular-prefix user_roles/role_permissions and user_permissions, expiry/revocation/history are absent. |
| B.6: authority rules and seeds | Partial | UserDetailServiceImpl expands ROLE_ roles and permissions; InitCore seeds ADMIN/USER. Direct GRANT/DENY, expiry, protected/inactive records, final-sign-in-method rule and complete seed permissions remain. |
| C.1–C.4: separate use cases | Missing | Existing controllers/authentication/initializers access framework objects and JPA repositories; specified commands/results/use-case classes are absent. |
| C.5: output ports | Missing | Existing repositories extend Spring Data directly. Plain application-owned repositories, PasswordHasher and CurrentActorProvider are absent. |
| Persistence / Liquibase | Partial | Five SQL migrations exist. Tests use an explicit H2 test changelog with a syntax adaptation. Production PostgreSQL migration execution and upgrade compatibility are not established by that test. |
| D.1–D.3: 39 API method-route pairs | Missing | Current controllers expose login and home only; user/role/permission/admin/self-service APIs absent. |
| D.4–D.5: DTOs, validation, pagination, Problem Details | Missing | Required REST contracts and exception mapping absent. |
| D: own Bearer JWT API security | Missing | SecurityConfig has authorization-server and browser chains; no dedicated JWT resource-server API chain or permission endpoint enforcement. |
| E.1: authorization server adapter | Partial | JdbcRegisteredClientRepository explicitly configured. No explicit JdbcOAuth2AuthorizationService or JdbcOAuth2AuthorizationConsentService beans; schema presence alone is insufficient. UserDetailsService bypasses required use case. |
| E.2: claims | Partial | TokenConfig adds roles/authorities and existing service-specific audiences. UUID user subject, explicit permissions claim and complete scope-controlled OIDC mappings remain. Preserve existing audience behavior during changes. |
| E.3: DCR | Missing | OIDC enabled with defaults, but no clientRegistrationEndpoint configuration or seeded client.create bootstrap flow. |
| E.4: four providers | Partial | Four registrations, login buttons, callback/cancellation tests, Telegram PKCE. No internal provisioning/linking. Tests do not complete real external token exchanges. |
| F: JTE pages | Partial | login.jte, home.jte and error.jte exist. Required index.jte profile and six other specified routes/templates remain. Existing home only shows principal username and sign-out. |
| F: English/Khmer | Missing | Templates contain English literals; no message bundles or language resolver/switcher. |
| Quality: tests | Partial | 13 tests pass; detailed coverage/limits below. |
| Delivery | Partial | README exists, but describes current demo behavior; no Postman collection. |
| Secrets | Conflict outstanding | Provider secrets are directly configured, matching earlier user preference but differing from assignment environment-only requirement. Do not copy secret values into reports. |

## Executed baseline

Command from microservices/: `./gradlew :iam-service:test`.
Result: BUILD SUCCESSFUL; 13 tests, zero failures/errors (11 application tests and 2 signing-key tests).

Coverage includes real HTTP login rendering, context-path links/notices, form credentials and CSRF,
authenticated home rendering, provider authorization starts/cancellation, Telegram PKCE,
OIDC discovery, JDBC client seeding/idempotency and signing-key persistence/corruption handling.

Limits: the test named authenticatedHomeRendersJteForProviderPrincipals uses a mock username/password principal,
not an actual OAuth2User/OidcUser. Provider-start tests do not establish real provider login/provisioning.
H2 tests do not prove PostgreSQL migration upgrades or assignment compliance. No full browser visual audit was run.

## Migration sequence and safeguards

1. Create modules and move code without resetting existing database or rewriting applied migration contents.
2. Introduce plain domain models and application ports; legacy JPA models move into persistence until replacement.
3. Replace direct persistence access through application use cases; remove temporary cross-adapter dependencies before marking architecture complete.
4. Add forward-only migrations to preserve users, client secrets, registrations and signing keys; preflight existing data against new uniqueness/nullability constraints.
5. Implement and test domain rules before exposing admin operations; ownership restrictions on self-service are mandatory.
6. Add exact REST contracts and separate Bearer API security from session/CSRF browser security.
7. Complete JDBC authorization persistence, scope-based claims, DCR and internal federation.
8. Complete required JTE pages and English/Khmer behavior, then delivery artifacts and final traceability audit.

Existing unrelated changes and user-deleted DESIGN.md/OAUTH2.md/UX-CONTRACT.md are preserved.
No live database, existing process, OAuth secret or signing key was modified during Phase 1.

## Decisions still requiring explicit treatment

- Mandatory user email/names may be unavailable from social providers: collect required profile data; do not fabricate verified identities or silently link accounts by unverified email.
- Assignment table detail omits general version/audit fields despite quality section requiring them everywhere: document additions and preserve immutable revocation history.
- Metadata protection on system roles versus permission-assignment modification must have a documented rule.
- Linking and connected-app revocation need supplementary browser action contracts; several admin/reset/revocation payloads are unspecified.
- Environment-only secrets conflict remains visible and must not be marked compliant while direct credentials remain.

## Phase 2: module structure — complete

The baseline table above describes Phase 1, before relocation. Current changes:

- Seven required leaf modules are declared in settings.gradle; the service root aggregates lifecycle/launch tasks.
- iam-main owns the Boot application, configuration, integration tests and existing core seeding.
- iam-web owns JTE, view configuration, controllers and static assets.
- iam-persistence owns existing JPA entities/repositories and unchanged Liquibase classpath resources.
- iam-infrastructure owns security/Authorization Server, signing keys and registered-client seeding.
- Application now exposes framework-free AuthUserResult/UserAuthenticationPort contracts; the JPA adapter implements the port and UserDetailsService consumes it. No adapter-to-adapter project dependency is needed.
- Domain-core and REST modules are structural foundations; their domain models and endpoints remain for subsequent steps. JPA entities now use persistence.entity; repositories/adapters, infrastructure and browser code use matching module namespaces. The empty legacy src tree has been removed.
- verifyModuleBoundaries enforces module dependency direction and checks inner-layer Java sources for framework imports.
- Existing service-root test/bootRun/bootJar commands delegate to the appropriate modules; all module tests participate in the root test task.
- JTE lookup covers iam-web, iam-main, service, microservices and repository launch directories.

Verification: service composite test + check + bootJar passed; 13 tests, zero failures/errors.
The full suite also passed with test working directory set to the repository root.
IntelliJ compilation passed with the existing unknown When.MAYBE annotation warning.
All six moved Liquibase files match their original Git contents byte-for-byte; no database reset or live DB mutation was performed.
Executable jar path: iam-main/build/libs/. Production JTE precompilation is still pending; packaged-jar browser rendering is not claimed.

## Phase 3: plain domain and Section B rules — complete

New code lives in iam-domain/iam-domain-core under istad.ha.iamservice.domain.model/rule/exception.
All seven principal models are immutable plain Java records: User, Role, Permission, Identity,
UserRole, RolePermission and UserPermission. Shared profile/address/audit/deletion/assignment value objects
represent every specified field, including UUIDs, nullable social-only password hashes, LocalDate birthdates,
Instant audit/login timestamps and last-persisted optimistic-lock versions.

Implemented behavior:

- Required fields/column-length limits, email normalization, en/km locales, IANA zones, E.164 phones,
  country codes, permission resource:action names and consistent metadata timelines.
- Account enable/lock states, failed-attempt recording/reset, password-hash changes and audited soft deletion.
- Protected system-role/permission metadata; inactive/deleted targets cannot be assigned.
- Explicit audited assignments with optional expiry, reversible snapshots but irreversible revoke operations,
  and no assignment-delete API. Revocation retains assignment actor/time/expiry; reason stores current rationale.
- Effective authorities: ROLE_ role names and active role/direct grants minus active direct denials.
  Time boundaries, revoked/expired paths, unrelated users and inactive/deleted records are filtered.
- Identity ownership, one provider per user, provider+subject uniqueness across supplied records,
  defensive raw-JSON copies and last-sign-in-method protection on both public unlink entry points.
- ADMIN/USER and 14 administrative permission seed definitions. Runtime database seeding remains unchanged.

The domain README records field coverage, the explicit USER-read seed policy, system-assignment policy,
identity historical-key reservation and the persistence contract. These choices do not claim database enforcement.

Verification: 29 domain tests plus the 13 existing application/signing-key regressions (42 total).
Full check/module-boundary verification and bootJar passed. IntelliJ compiled with the existing When.MAYBE warning.

Limits intentionally left to following steps: global uniqueness/concurrency must be enforced transactionally and
by database constraints; persistence advances the loaded version on successful save; new records are not yet
mapped to JPA or used by production login. Existing legacy JPA @ManyToMany mappings and live database schema
are unchanged in this phase. Consequently complete Section B database compliance is still pending Step 4.

## Phase 4: persistence and migrations — complete

- Added framework-free repository ports for users, roles, permissions, identities and assignments.
- Replaced all legacy JPA classes with complete *Entity mappings, shared auditing/deletion metadata,
  @Version, JSONB identity attributes and explicit assignment entities; no @ManyToMany remains.
- Added port adapters with flush/refresh version round trips, immutable history guards, protected system
  metadata and owner-row locking for transactional identity changes.
- Added forward-only 006 migrations with preflight checks, UUID conversion, required constraints,
  preserved legacy join-table archives, copied audited assignments and PostgreSQL assignment-delete triggers.
- All original five SQL migrations remain unchanged. User IDs/password hashes, OAuth client records and
  existing relationships are preserved by upgrade tests. No live db_oauth2 migration was performed.
- Bootstrap now uses domain seed definitions and ports, preserves user passwords and historical revocations,
  and avoids recreating deleted bootstrap users. Local authentication uses domain effective permissions,
  accepts username/email, and safely rejects password login for social-only users.

Verification: PostgreSQL 18.4 suite passed all 62 tests, including upgrade/idempotency/data-preservation,
preflight failures, uniqueness, optimistic concurrency, parallel unlink protection and all three history triggers.
Default H2 suite passed 59 tests; one PostgreSQL-only parameterized container skipped (60 reported tests).
check, module-boundary verification, bootJar and IntelliJ compilation passed. The existing When.MAYBE warning remains.
The persistence README documents transaction, migration and concurrency contracts.

Application use cases, general REST APIs and complete federation are still pending. Identity use cases must
use the provided owner lock inside their outer transaction; the integration test demonstrates that contract.
Production JTE precompilation and the earlier environment-only credential compliance decision remain pending.

## Phase 5: application use cases — complete

- Implemented all 39 named Section C classes (19 user, 9 role, 7 permission, 4 identity use cases), with commands,
  partial-update types, queries, safe results and framework-free transaction orchestration.
- Added PasswordHasher/CurrentActorProvider infrastructure adapters and completed SQL-backed pagination/filter/sort ports.
- Spring wiring constructs each plain use case explicitly. UserDetailsService now delegates to
  LoadUserForAuthenticationUseCase and retains the internal UUID in a credentials-erasing principal.
- Local form-authentication success/failure events invoke RecordLoginAttemptUseCase.
- Registration assigns USER only; admin creation validates requested roles. Password changes/resets, account lifecycle,
  profile verification resets, protected metadata and audited assignment/revocation rules are implemented.
- Identity operations lock the owner, enforce ownership and last-method protection, and restore historical identities
  only through explicit relinking. Federation does not auto-merge email matches or trust provider role claims;
  missing mandatory fields return profile_completion_required without creating fake account data.
- Multi-write operations roll back atomically. Concurrent first federation transactions converge on the exact same
  provider identity/internal account through a guarded conflict retry.

Verification: all 39 classes are exercised by tests; 84 PostgreSQL tests passed with no failures/skips.
Default H2: 81 passed, 1 PostgreSQL-only container skipped (82 reported); no failures.
IntelliJ compilation, module-boundary checks and bootJar passed; the existing When.MAYBE warning remains.
jdeps confirms the application jar references only java.base and iam-domain-core. No SQL migrations or live
application database were changed in Step 5. The application module README documents policies/trust boundaries.

REST endpoints/security and actual provider callback provisioning remain for subsequent steps. AuthUserResult is
internal and contains a hash; only safe application result types should be serialized by the REST adapter.

## Phase 6: secured REST APIs — complete

- Added all 39 Section D method-route pairs in iam-rest-api: registration, users, roles, permissions and /api/me.
- Controllers map dedicated validated DTOs to the existing use cases. PATCH preserves omission vs explicit null;
  unknown fields and privilege injection are rejected. Public projections match Section D.5 and exclude credentials
  and raw provider attributes. Paging/filter/sort validation stays in existing application queries.
- Added a stateless /api/** chain between Authorization Server and browser chains. It verifies the local signing key,
  configured issuer and token lifetime, requires exp/access-token scope and rejects ID tokens. Browser sessions cannot
  authorize APIs. Browser login/OAuth/session/CSRF behavior remains covered by regressions.
- Administrative methods require exact resource:action claims; scopes or ROLE_ADMIN alone cannot bypass permissions.
  /api/me derives the internal UUID from the verified subject. Other users' identity UUIDs cannot be unlinked through it.
- Local-user access tokens now use the internal UUID subject so the real Authorization Code + PKCE flow can call /api/me.
  Service-client subjects remain unchanged. Complete scope-controlled OIDC claims remain Phase 7.
- Added RFC 9457 validation/application errors, JSON 401/403 responses, 404 lookups and 409 conflict handling.
  Revocation requires its existing nonblank audited reason; parser errors never echo password values.

Verification: check (including verifyModuleBoundaries) and bootJar passed. H2: 126 reported tests, 125 passed,
one PostgreSQL-only container skipped, zero failures/errors. The 44 new REST/security tests include real signed tokens,
all 33 administrative routes with missing/wrong/exact permissions, successful workflows for all 39 routes, safe response
shapes, validation, paging, ownership, account/password/assignment operations and a real Authorization Code + PKCE
exchange followed by /api/me; the same flow's ID token is rejected. PostgreSQL re-verification was unavailable because
both local Docker sockets were absent. Earlier Phase 5 PostgreSQL evidence is historical, not a new Phase 6 run.
No live IAM database, credentials or signing keys were changed. No provider callback provisioning or deployment occurred.

## Phase 7: Authorization Server persistence, OIDC claims and DCR — complete

- Explicit JDBC registered-client, authorization and consent services use the existing schema. Authorization JSON
  uses Spring Security's restricted Jackson 3 mapper with only the final internal principal and Long timestamp added.
  Principal snapshots exclude passwords/hashes; codes, tokens and consent reload through newly constructed services.
- Local-user access and ID tokens use the internal UUID. Access tokens include freshly evaluated roles, permissions
  and authorities; refresh re-evaluates grants and account flags. Existing service subjects/audiences are preserved.
- Complete scope-controlled ID-token claims implement profile/email/phone/address; optional values are omitted,
  verification flags use stored values, birthdate is ISO and updated_at is epoch seconds. UserInfo uses native scope
  filtering and excludes role/permission/authentication/provider data.
- Enabled native POST/GET /connect/register. New client-registrar seed has only client.create and a separate required
  IAM_REGISTRATION_CLIENT_SECRET. Existing clients, scopes and secret hashes are preserved, with no silent rotation.
- Registration uses persisted active single-scope initial/registration tokens, consumes the initial token once, returns
  the generated plaintext secret only on creation, hashes storage, and restricts configuration reads to their own client.
  GET omits stored secret hashes. Native redirect/JWKS checks are retained alongside a supported-OIDC-scope policy
  that rejects privileged scopes and machine-to-machine grants. New OIDC clients require PKCE and user consent.
- Added 19 protocol integration cases covering complete/limited claims and UserInfo privacy, JDBC reconstruction,
  consent, refresh narrowing/permission changes/rotation, disabled users, service compatibility, registration replay,
  own-client reads, invalid tokens/scopes/metadata and seed idempotency.

Verification: check (including verifyModuleBoundaries) and bootJar passed. H2: 145 reported tests, 144 passed,
one PostgreSQL-only container skipped, zero failures/errors. All 44 Step 6 REST cases remain green. Reconstruction
checks prove JDBC reloads; a full OS-process restart and a new PostgreSQL run are not claimed. No live IAM database
was migrated, no existing client/provider secrets or signing keys were rotated, and no deployment occurred.

## Next phase

Phase 8: Section E.4 provider callbacks/provisioning and authenticated account linking for Google, GitHub, Facebook
and Telegram. Then the remaining JTE pages/localization, production template compilation and final delivery/audit.


