# Resume IAM assignment implementation

Saved 6 November 2026 at the user's request: continue this work when we meet again.

## Resume point

Steps 1–7 are complete. Next: **Step 8 — Section E.4 provider callback provisioning and account linking**.
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

Historical Step 5 verification: PostgreSQL 84 tests passed with zero failures/skips; H2 81 passed,
one PostgreSQL-only parameterized container skipped (82 reported); module checks, bootJar and IntelliJ compilation passed.
Existing compiler warning: unknown enum constant When.MAYBE.
jdeps shows application production code depends only on java.base and iam-domain-core.
All 39 use cases are referenced by tests. Temporary PostgreSQL test databases were removed after verification.
No live IAM database migration, signing-key rotation, provider-secret change, or deployment was performed.

## Next implementation scope

Step 7 is complete. The user asked to save completion notes and resume next time.
Do not repeat the completed phases without a concrete regression.

Next: implement Section E.4 callback integration for Google/google, GitHub/github, Facebook/facebook and
Telegram/telegram. Validate each provider's proof before constructing FederatedIdentityCommand, route login through
FindOrProvisionFederatedUserUseCase, and route explicit authenticated account linking through LinkIdentityUseCase.
Bind linking callbacks to the authenticated internal owner and protect state/session/PKCE. Never merge matching emails,
trust provider role claims or fabricate required profile data. Missing mandatory provider fields require a controlled
profile-completion flow. Preserve browser CSRF and the existing last-sign-in/identity-history rules.

Then complete the required JTE pages, English/Khmer localization, production template precompilation and final
README/Postman/requirements audit. Read iam-infrastructure/README.md for current OIDC/DCR contracts first.

Current verification (Step 7): 145 reported H2 tests, 144 passed, one PostgreSQL-only container skipped; zero failures/errors.
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
- User prefers direct YAML provider credentials; assignment requires environment-only secrets. This conflict remains
  recorded for explicit treatment. Do not silently rotate, expose, or copy secrets into notes/reports.
- Preserve existing unrelated edits and user-deleted DESIGN.md, OAUTH2.md and UX-CONTRACT.md. Do not recreate them.
- Work is uncommitted; do not reset or overwrite the user's workspace changes.
- Original SQL migrations 001–005 are unchanged. New 006 changesets upgrade on the next IAM restart.
  Several columns were renamed; the old binary must be stopped before using the new schema.
- Identity writes need an outer write transaction and owner lock. TransactionRunner handles this in existing use cases.
- Provider+subject and owner+provider keys remain reserved after unlinking. Explicit relinking restores the same row.
- Federation never auto-merges email matches, trusts provider role claims, or fabricates mandatory profile data.
- New-password policy: minimum 12 characters, maximum 72 UTF-8 bytes for BCrypt. Existing demo passwords remain unchanged.
- JTE uses development compilation; production jar template precompilation is still pending.
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
