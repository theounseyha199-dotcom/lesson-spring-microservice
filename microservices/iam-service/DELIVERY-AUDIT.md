# IAM delivery audit — 6 November 2026

The implementation and automated delivery checks pass. Live Google/GitHub/Facebook/Telegram account acceptance is
**not performed** and remains the human acceptance item. No grade or full external-provider acceptance is claimed.
The original 14-page assignment PDF was re-read with pdftotext; its transcription remains in ASSIGNMENT-CHECKLIST.md.

## Requirement evidence

| Requirement | Evidence | Audit result |
| --- | --- | --- |
| Objective / A: seven modules and adapter direction | settings.gradle; build.gradle verifyModuleBoundaries; framework-free domain/application scan | Pass |
| B: complete users/roles/permissions/identities and audited assignments | Domain models/rules, explicit JPA entities, migration 006; PostgreSQL mapping/uniqueness/audit/history/concurrency tests | Pass |
| C: required use cases and output ports | All 39 names from the assignment map to source classes; three additional browser-support use cases use a plain port | Pass |
| D: REST routes, payloads, authorities and safe outputs | All 39 controller method/route pairs match the new Postman collection; integration tests exercise permission/ownership and RFC 9457 boundaries | Pass |
| E.1–E.3: JDBC OAuth state, scoped claims, DCR | AuthorizationServerIntegrationTests, persistent signing keys, controlled single-use registration/read tokens | Pass |
| E.4: four validated provider adapters and explicit linking | FederationIntegrationTests: real callback filters, local provider exchanges and signed token validation | Implementation passes; live accounts pending |
| F: eight JTE pages and en/km bundles | Exact required template names; 133 matching message keys; browser/ownership/consent tests | Pass |
| Quality: secrets supplied externally | Required placeholders, bootstrap/client injection, empty-secret Postman exports, Git-ignored local values | Resolved in this audit |
| Delivery: tests, explanations, README and Postman | Linked module guides, final README, complete collection/environment and reusable audit script | Artifacts ready |

Production templates are generated and compiled by JTE 3.2.5 during the Gradle build. The actual JAR was exercised
from /tmp without template sources. Existing SQL migrations 001–005 match their Git baseline; only forward migration
006 upgrades domain data. Framework-free domain/application sources and project dependency direction pass checks.

## Final verification

| Verification | Result |
| --- | --- |
| Fresh disposable PostgreSQL 18.4: check bootJar --rerun-tasks | 184 passed; 0 failures/errors/skips; test database removed in finally |
| H2: check bootJar | 182 reported: 181 passed, 1 PostgreSQL-only container skipped; 0 failures/errors |
| Postman/Newman against isolated packaged JAR | 44 requests; 66 assertions; 0 failures |
| scripts/audit-delivery.py | All 9 contract checks pass |
| Browser evidence from Step 9 | 48 checks; 0 JS errors; en/km, desktop/mobile, native controls and 200% zoom |

The Postman runtime used a synthetic internal administrator obtained through the actual authorization-code + S256
PKCE flow. It exercised metadata, public registration, permissions, roles, user CRUD/assignments, own-account reads/
profile editing, DCR, cleanup and negative cases. Interactive OAuth-helper and manual password/identity folders were
excluded from that run; server integration tests cover those REST routes. No filled environment or runtime tokens
were exported. The production JAR contains no H2/test code; H2 and test changelogs were supplied only to the isolated
smoke process through an external loader path.

## Corrections made by the audit

- Provider IDs/secrets, database password, bootstrap account passwords and client secrets now use external configuration.
  Current active values were retained in protected Git-ignored local files, without credential rotation or changing
  existing stored hashes. The legacy service Postman export's filled secret was removed and its prior private environment
  retained locally. New exports contain empty secrets/tokens/passwords.
- Fresh OIDC seeds now honor the configured client ID, callback and logout URIs. An added test verifies the configuration
  is actually consumed. Existing registered clients remain untouched; changes to seed configuration do not rotate or
  widen them. The formerly unused IAM_USERNAME setting was removed instead of promising unsupported seed renaming.
- A dedicated IAM Postman collection now covers all 39 assignment REST routes. Request bodies, DELETE revocation reasons,
  UUID capture, lifecycle ordering, auth separation and response assertions were checked and exercised.
- The service/Postman READMEs now describe the final behavior and runnable commands, replacing obsolete implementation
  notes and a reference to a nonexistent run-all.sh. Module guides retain historical phase evidence where relevant.

## Explicit decisions and limits

The user delegated the secret-policy decision; environment-supplied secrets were chosen over the earlier direct-YAML
preference. Git-ignored local properties remain a workstation convenience and are not packaged delivery credentials.
This removes literals from the current delivery but does not rewrite prior Git revisions or rotate third-party secrets.
Secret-history remediation or rotations, if needed, must be managed separately with the affected provider/account owners.

Provider attribute snapshots intentionally omit protocol tokens/nonces and provider privilege inputs; the stored JSONB
column holds provider profile metadata. Missing mandatory social fields require profile completion rather than invented
names/emails. Matching emails never auto-merge; existing identity ownership/history is reserved after unlinking.

New passwords require at least 12 characters and at most 72 UTF-8 bytes; bootstrap values/existing hashes are preserved
rather than silently reset. Assignment revocation keeps audit rows. Revoking an app removes its stored authorizations
and consent, preventing refresh/code reuse; self-contained access JWTs may remain valid downstream until expiry.
Native date-picker chrome follows the browser locale; application labels follow en/km. Khmer text has working bundle/
rendering coverage, not an independent native-language editorial review.

## Remaining human acceptance

Follow [LIVE-PROVIDER-ACCEPTANCE.md](LIVE-PROVIDER-ACCEPTANCE.md) using the real provider accounts, registered callback
origins and current credentials. Record nonsecret outcomes per provider. A redirect-only check is not a successful
login. No external-provider password/MFA interaction, live database reset, deployment, commit or push was performed.

The grading/deadline row remains an assignment rubric, not a self-awarded score. Final submission approval and live
provider acceptance remain with the owner.


## Live-provider readiness follow-up

A subsequent real authorization-page check found/fixed HTTP callback generation behind HTTPS ngrok. Native Tomcat
forwarded-header handling now produces HTTPS, with direct HTTP preserved in an embedded-server regression. Full H2:
182 reported, 181 passed, one skipped; packaging passes. GitHub/Facebook/Telegram reach sign-in pages. Google localhost
reaches sign-in, while its public callback must be added to the provider console. See LIVE-PROVIDER-RESULTS.md.
These checks did not authenticate a provider account or create live identities. Existing DB counts/history were read
only. The local IAM process was restarted to apply this configuration; no provider/client credential or signing-key rotation occurred.

PostgreSQL was rerun after the proxy callback fix: 184 passed, no failures/errors/skips; the disposable database was removed.
