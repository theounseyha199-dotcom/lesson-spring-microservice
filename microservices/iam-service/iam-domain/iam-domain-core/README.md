# IAM domain core

Plain Java 17 records and rules for assignment Section B. No Spring, JPA, web or serialization annotations.
Models are immutable; a state transition returns a replacement snapshot and retains the original history.

## Field coverage

- `User`: internal Long id, UUID public subject, username, lower-case email and verification, nullable password hash, all four account flags, failed-login count, last-login/password-change instants, profile, deletion and audit metadata.
- `UserProfile`: given/family/middle names, nickname, gender, LocalDate birthdate, profile/picture/website URLs, IANA zoneinfo, en/km locale, E.164 phone and verification, and Address.
- `Address`: streetAddress, locality, region, postalCode and ISO two-letter country.
- `Role`: id, UUID, name, description, system/active flags, deletion and audit.
- `Permission`: id, UUID, name (= resource:action), resource, action, description, system/active flags, deletion and audit.
- `Identity`: id, UUID, user id, provider/subject, optional email and verification, display name, provider username, picture, deeply immutable JSON attributes, linked/last-login instants, deletion and audit.
- `UserRole`, `RolePermission`, `UserPermission`: internal id, explicit foreign-key IDs, shared AssignmentMetadata; UserPermission additionally contains GRANT/DENY effect.
- `AuditMetadata`: createdAt/createdBy, updatedAt/updatedBy, and last-persisted optimistic-lock version.
- `SoftDeletion`: deleted flag, deletedAt/deletedBy.
- `AssignmentMetadata`: assignedAt/assignedBy, expiresAt, revokedAt/revokedBy, reason and audit/version. Assignments have no delete operation.

## Rules and persistence contract

`AuthorityResolver` derives ROLE_ authorities and permissions from current active records and assignment snapshots.
The evaluation time is supplied explicitly; expiry/revocation are exclusive boundaries. Direct DENY wins over all grants.
Future assignments and assignments for other users cannot contribute authorities. Disabled/deleted users receive none.

`IdentityRules` enforces ownership, one identity/provider/user, provider+subject uniqueness, and last-sign-in-method protection.
Call it with the relevant complete identity set inside a transaction. Uniqueness includes soft-deleted rows to match the
specification's unique keys; re-linking a historical provider identity must restore/update that record in a later use case,
rather than insert a duplicate. Both Identity.unlink and IdentityRules.unlink require the owner and identity set, so the public lifecycle API
evaluates last-sign-in-method protection before returning the soft-deleted snapshot.
Missing required user profile fields are rejected; no fake email/name or provider verification is fabricated.

System role/permission metadata and lifecycle changes are prohibited. Assignments are separate audited records;
active system roles/permissions may participate in assignments. The seed definitions give ADMIN all 14 user/role/permission
administrative permissions, and USER the three read permissions. Self-service authorization remains separately authenticated.
The specification does not prescribe the exact USER permission set; this is an explicit seed policy for later wiring.

Mutation methods update actor/time but preserve AuditMetadata.version. The persistence adapter must compare that
loaded version, advance it atomically on successful save, and return the persisted version. This module alone does not
implement database concurrency or global uniqueness checks. These belong to Step 4.

Soft deletion keeps the row; assignment revocation keeps the original assignment actor/time and expiry.
The single assignment reason column records the current lifecycle rationale (including the supplied revocation reason).
Password hashing/checking, email uniqueness, ID allocation, provider attribute mapping, and transaction/locking behavior
belong to adapters/application use cases. Domain password APIs accept hashes only.

## Verification

Run from iam-service: `./gradlew :iam-domain:iam-domain-core:test verifyModuleBoundaries`.
Tests cover grants/denials, account states/login attempts, metadata protection, expiry/revocation, identities,
field validation, audit/deletion history and defensive copies. Step 4 maps these records in iam-persistence and uses them for local authentication;
see ../../iam-persistence/README.md for migration and database enforcement details.
