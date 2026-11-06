# IAM persistence (Step 4)

JPA adapters implement application-owned UserRepository, RoleRepository, PermissionRepository,
IdentityRepository and AssignmentRepository ports. All seven tables have explicit entities;
there are no @ManyToMany mappings. DomainMapper maps every domain field, including profile/address,
JSONB provider attributes, deletion metadata and audit/version values.

## Storage and concurrency

- All seven entities inherit @Version and creation/update actor/time columns. Save compares the loaded
  version, flushes and refreshes the managed entity before returning its persisted domain snapshot.
- Database constraints enforce UUIDs, unique username/email/phone, role/permission names, both identity
  uniqueness rules, foreign keys, assignment effects and lifecycle shape. Lower-case email is checked.
- Creation audit, public UUIDs, assignment target/assignment audit and linked identity keys cannot be
  rewritten through adapters. System role/permission metadata is protected even against reconstructed snapshots.
- Soft-deleted users/roles/permissions are excluded from ordinary lookup; audit records remain stored.
  Identity lookups include historical rows because provider keys remain reserved after unlinking.
- Assignments retain history and are revoked, never deleted. PostgreSQL BEFORE DELETE triggers protect
  all three assignment tables. No cascading foreign-key deletion is used in the new assignment tables.
- UserRepository.findByUuidForUpdate requires an outer transaction and holds a pessimistic owner-row lock.
  Identity link/unlink use cases must obtain this lock before reading the identity set and changing it.
  A concurrent integration test proves that two unlinks cannot remove every sign-in method.
- Repository ports do not expose physical delete operations. Spring Data repositories are adapter internals.
  Step 5 application use cases supply actor/clock values and transaction boundaries through framework-free ports.

## Upgrade

Original 001–005 SQL files and their classpath locations are unchanged. New 006-domain-model.yaml
performs preflight validation, converts user UUID storage, expands the domain schema and creates identities
and audited assignments. SQL is outside the includeAll directory so it executes only through its changeset.
Preflight covers required legacy user data, known field-length reductions, normalized-key collisions,
permission syntax and email/phone/gender formats. It halts on incompatible data; it does not merge accounts
or truncate fields. Invalid UUID conversion also stops the upgrade.

User primary keys, UUID values, password hashes, existing role/permission IDs and OAuth client records are
preserved. Names and emails are normalized; birthdate becomes DATE. The unused legacy cover column remains.
Legacy join tables are retained as legacy_users_roles and legacy_roles_permissions; relationships are copied
into the new audited tables with migration attribution. Previous assignment time/actor cannot be recovered
from unaudited tables, so the copy explicitly records migration time/actor rather than claiming an original grant time.

Run only the new application version after stopping the old one: renamed columns are incompatible with the
old binary. Liquibase applies the upgrade on the next IAM startup. Back up the application database before
applying a schema upgrade. This implementation run migrated only isolated test databases, not db_oauth2.
No signing keys, provider credentials or existing registered-client secrets were rotated.

## Seeding and authentication

Bootstrap consumes SystemDefaults through the output ports. It preserves existing users/passwords, does not
recreate soft-deleted bootstrap usernames, and does not re-grant previously revoked/expired role-permission
pairs. Missing ADMIN/USER definitions and missing default permissions are inserted idempotently.
Local authentication now calls LoadUserForAuthenticationUseCase and resolves role/direct authorities through the domain AuthorityResolver, including DENY,
expiry, revocation and inactive/deleted records. Lookup accepts username or email. Social-only users cannot
use password authentication. Provider callback provisioning/linking is implemented through infrastructure in Step 8.

## Tests

From iam-service, ./gradlew check bootJar runs the H2 suite. It uses the original H2 adaptation for migration
005 plus the exact new 006 changesets (with dialect-specific UUID syntax/function replacement).
Hibernate schema validation is enabled in the integration tests.

For PostgreSQL, provision a disposable database named iam_test_..., then supply IAM_TEST_DB_URL,
IAM_TEST_DB_USER and IAM_TEST_DB_PASSWORD when running the same Gradle command. The harness rejects URLs
outside that database naming convention. Upgrade tests create/drop their own randomly named schemas inside
that disposable database; they never target the application's live schema.

Historical Step 4 verification on PostgreSQL 18.4: 62 tests, zero failures/skips. H2: 59 passed, one PostgreSQL-only parameterized
container skipped (60 reported tests). Coverage includes fresh startup, migration from the legacy schema,
repeat migration, preservation of credentials/clients/relationships, rejection of collisions and overlength data,
profile/JSON round trips, database uniqueness, stale snapshots, concurrent writes, concurrent unlinking,
assignment deletion triggers, audit/version changes and seed idempotency. Existing browser/OAuth-start tests
remain included; real provider token exchange is outside this step.

Final Step 10 verification reran the full IAM suite on a fresh disposable PostgreSQL 18.4 database: 183 passed, zero failures/errors/skips; the database was removed. See ../DELIVERY-AUDIT.md.
