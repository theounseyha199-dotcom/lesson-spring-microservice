# Resume IAM assignment implementation

Saved 6 November 2026 at the user's request: continue this work when we meet again.

## Resume point

Steps 1–9 and the automated Step 10 delivery audit are complete. Remaining: **manual live-provider acceptance and submission review**.
Do not restart the implementation or repeat completed phases without a concrete regression reason.

Read first:

- ASSIGNMENT-CHECKLIST.md — assignment requirements and full PDF transcription.
- IMPLEMENTATION-STATUS.md — baseline, completed phases, evidence and remaining work.
- iam-domain/iam-application-service/README.md — all 39 use cases, input/result contracts and trust boundaries.
- iam-persistence/README.md — migrations, versioning, uniqueness and locking contracts.

Original assignment: /home/seyha/Downloads/III. Assignment - Spring Authorization Server & IAM Service.pdf
Deadline stated in the PDF: 10 November 2026, 11:59 AM; late penalty 25%.

## Completed

1. Baseline audit.
2. Seven-module Gradle structure; packages now match module ownership; old empty src removed.
3. Plain Java domain models and Section B rules.
4. Complete JPA adapters, audited assignment entities, forward-only migrations and domain-based local authentication.
5. All 39 Section C use cases, typed commands/results/PATCH inputs, paged queries, password/actor/transaction ports,
   Spring wiring, UUID-based credentials-erasing local principals and login-attempt recording.
6. All Section D REST APIs, own-issuer bearer security, permission/ownership enforcement and API integration tests.
7. JDBC authorization/consent persistence, complete scope-controlled OIDC claims, and controlled Dynamic Client Registration.
8. Validated provider callbacks, internal social-user sessions, mandatory-profile completion and state/owner-bound account linking.
9. All Section F JTE pages, provider disconnection, app revocation, English/Khmer localization and production template compilation.
10. Automated assignment/README/Postman audit, external secret configuration, fresh PostgreSQL/H2 validation and executed collection checks. Human provider-account acceptance remains pending.

Historical Step 5 verification: PostgreSQL 84 tests passed with zero failures/skips; H2 81 passed,
one PostgreSQL-only parameterized container skipped (82 reported); module checks, bootJar and IntelliJ compilation passed.
Existing compiler warning: unknown enum constant When.MAYBE.
jdeps shows application production code depends only on java.base and iam-domain-core.
All 39 use cases are referenced by tests. Temporary PostgreSQL test databases were removed after verification.
No live IAM database migration, signing-key rotation, provider-secret change, or deployment was performed.

## Next implementation scope

The automated delivery work is complete. Read README.md, DELIVERY-AUDIT.md and LIVE-PROVIDER-ACCEPTANCE.md.
Do not repeat implemented phases or fabricate live login success. The remaining work needs the real provider account
owner to authenticate/MFA and verify provider-console callbacks. Record nonsecret outcomes only. No deployment,
commit or push has been requested; final submission/grading remains with the owner.

Current verification (Step 10): PostgreSQL 18.4: 183 passed, no failures/errors/skips; disposable database removed.
H2: 181 reported, 180 passed, one PostgreSQL-only container skipped, no failures/errors. check/module boundaries and
bootJar pass. The new IAM Postman collection passed 44 requests/66 assertions against an isolated packaged app.
All 9 delivery contract checks pass. Step 9 browser evidence is 48 passing checks. No live application DB writes/reset,
provider-account login, credential rotation, deployment, commit or push occurred during this audit.

External secret configuration is now the chosen policy. Values were retained in protected, ignored local files;
new exported Postman environments have empty secrets. Existing stored passwords/client secrets are not changed by
startup configuration. Fresh OIDC seeds honor the configured ID/callbacks. Use the dev profile only for live JTE editing.

Historical verification (Step 9): 180 reported tests, 179 passed, one PostgreSQL-only container skipped; no failures/errors.
All 12 new browser cases pass; check/module boundaries and bootJar pass. The production JAR was run from /tmp with
no template sources and an isolated H2 database using test-only external loader resources. Chromium passed 48 checks
with zero JS errors; desktop/mobile, English/Khmer, native controls and 200% zoom were checked. Strict UI audit has
zero findings. No live database, existing provider/client credentials, signing key or deployment was changed.

Historical verification (Step 8): 168 reported tests, 167 passed, one PostgreSQL-only container skipped; zero failures/errors.
All 23 new federation protocol/security cases pass. check (including module boundaries) and bootJar pass. Strict UI
audit has zero findings; actual rendered JTE pages were inspected in Firefox at mobile/desktop widths. No live provider
account login, new PostgreSQL run, deployment, provider/client secret rotation or signing-key change was performed.

Historical verification (Step 7): 145 reported H2 tests, 144 passed, one PostgreSQL-only container skipped; zero failures/errors.
19 new Authorization Server protocol cases and all 44 REST/security cases pass. check (including module boundaries)
and bootJar pass. JDBC service reconstruction reloads codes/tokens/consent; no full OS-process restart is claimed.
PostgreSQL was not re-verified because local Docker sockets were unavailable in this session. No live db_oauth2
migration, deployment, existing credential rotation or signing-key change was performed.

New required startup configuration: set IAM_REGISTRATION_CLIENT_SECRET to a separate strong secret. It seeds
client-registrar once with only client.create; subsequent property changes do not rotate a stored secret. Existing
registered clients and their scopes are preserved. Fresh oidc-client seeds support all five OIDC scopes.
DCR accepts only the supported OIDC scopes and auth-code/refresh flows; initial registration tokens are single-use,
GET configuration requires the matching registration token, and stored secret hashes are never returned.
Local-user access/ID-token subjects are internal UUIDs; service-client subjects/audiences are preserved. Access-token
permissions are refreshed from current domain rules; ID-token/UserInfo personal claims are scope-controlled.

## Important contracts and preferences

- Set IAM_REGISTRATION_CLIENT_SECRET before starting IAM; keep its plaintext value out of notes and Git.
- On 6 November the user delegated the final choice: use external/environment-supplied secrets. The earlier direct-YAML preference is superseded. Preserve current local values and never rotate/expose them silently.
- Preserve existing unrelated edits and user-deleted DESIGN.md, OAUTH2.md and UX-CONTRACT.md. Do not recreate them.
- Work is uncommitted; do not reset or overwrite the user's workspace changes.
- Original SQL migrations 001–005 are unchanged. New 006 changesets upgrade on the next IAM restart.
  Several columns were renamed; the old binary must be stopped before using the new schema.
- Identity writes need an outer write transaction and owner lock. TransactionRunner handles this in existing use cases.
- Provider+subject and owner+provider keys remain reserved after unlinking. Explicit relinking restores the same row.
- Federation never auto-merges email matches, trusts provider role claims, or fabricates mandatory profile data.
- New-password policy: minimum 12 characters, maximum 72 UTF-8 bytes for BCrypt. Existing demo passwords remain unchanged.
- JTE defaults to packaged precompiled templates. Activate the dev profile only for live template editing.
- Facebook/Telegram currently use a configured ngrok callback. Start login on that same origin to retain OAuth state.
- Java entry point remains istad.ha.iamservice.IamServiceApplication in iam-main. Reload Gradle in IntelliJ if needed.

## Commands

From microservices/: ./gradlew :iam-service:check :iam-service:bootJar
From iam-service/: ./gradlew check bootJar
Run: ./gradlew bootRun from iam-service (root task delegates to iam-main).
Jar: iam-main/build/libs/.

Default tests use H2. Optional PostgreSQL tests require a disposable database named iam_test_... plus
IAM_TEST_DB_URL, IAM_TEST_DB_USER and IAM_TEST_DB_PASSWORD. The harness rejects application-database URLs.
Use isolated test databases; never point tests at db_oauth2. Existing local container ite-postgres exposes port 9543.
Gradle may need sandbox escalation for its home-directory cache; run IDE builds and Gradle verification sequentially
because concurrent compilers previously interfered with generated output.


## Live-provider readiness follow-up (6 November)

Tested all four real authorization pages. GitHub/Facebook/Telegram reach account sign-in; Google reaches account
sign-in from localhost but returns redirect_uri_mismatch from the public ngrok origin. LIVE-PROVIDER-RESULTS.md gives
the exact public URI to add to the configured Google Web OAuth client; retain the working localhost URI. No provider
console was modified and no provider password/OTP/MFA was entered. Existing DB rows have GitHub/Telegram login history,
a Google identity without a login timestamp, and no Facebook identity; these are historical evidence, not a new login.

Fixed a concrete readiness regression: enable Tomcat native forwarded headers so Google/GitHub callbacks use HTTPS
behind ngrok. New embedded-server test passes; full H2 suite: 182 reported, 181 passed, one skipped. IAM was restarted
through its IntelliJ run configuration and all public callback schemes rechecked. Keep the current local IAM process
running for the owner's manual tests. Live provider-account acceptance is still pending; do not fabricate completion.

Final proxy-fix PostgreSQL regression: 184 passed, zero failures/errors/skips; disposable test DB removed.
