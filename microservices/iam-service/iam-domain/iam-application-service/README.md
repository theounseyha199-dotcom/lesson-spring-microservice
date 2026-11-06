# IAM application service (Step 5)

Implements every use case in assignment Section C. Production sources depend only on the plain Java domain
and JDK; Spring configuration and transaction/security implementations live in iam-infrastructure.
One use case is one class, with an execute method and constructor-injected UseCaseContext.
The context contains ports, an injected Clock and shared lookup/projection helpers; individual classes own command orchestration.

## Use-case inventory

### C.1 Users

- RegisterUserUseCase
- CreateUserUseCase
- GetUserUseCase
- ListUsersUseCase
- UpdateUserProfileUseCase
- ChangePasswordUseCase
- ResetUserPasswordUseCase
- EnableUserUseCase
- DisableUserUseCase
- LockUserUseCase
- UnlockUserUseCase
- DeleteUserUseCase
- AssignRolesToUserUseCase
- RevokeRoleFromUserUseCase
- GrantPermissionsToUserUseCase
- RevokePermissionFromUserUseCase
- GetUserAuthoritiesUseCase
- LoadUserForAuthenticationUseCase
- RecordLoginAttemptUseCase

### C.2 Roles

- CreateRoleUseCase
- GetRoleUseCase
- ListRolesUseCase
- UpdateRoleUseCase
- ActivateRoleUseCase
- DeactivateRoleUseCase
- DeleteRoleUseCase
- AssignPermissionsToRoleUseCase
- RevokePermissionFromRoleUseCase

### C.3 Permissions

- CreatePermissionUseCase
- GetPermissionUseCase
- ListPermissionsUseCase
- UpdatePermissionUseCase
- ActivatePermissionUseCase
- DeactivatePermissionUseCase
- DeletePermissionUseCase

### C.4 Identities

- LinkIdentityUseCase
- UnlinkIdentityUseCase
- ListUserIdentitiesUseCase
- FindOrProvisionFederatedUserUseCase

## Inputs and outputs

Commands live in application.command, queries in application.query, and response-safe results in application.result.
AuthUserResult is deliberately authentication-only: it includes the internal UUID and password hash for the security
adapter and must never be serialized as a REST response. Other result types exclude passwords/hashes and raw provider attributes.
Password-bearing commands, AuthUserResult, federation commands and the session principal have redacted toString methods.
Authenticated session principals implement CredentialsContainer so Spring erases the password credential after authentication.

ProfilePatch uses FieldUpdate to distinguish omitted fields from explicit nulls. Omitted fields are preserved;
optional values can be cleared; required fields cannot. Address is a complete optional value replacement.
Changing a phone resets phone verification. Changing email normalizes it and clears email verification; profile
commands cannot set verification flags. Role/permission metadata updates use the same explicit PATCH-field distinction.

List queries enforce page >= 0, size 1..100 and whitelisted sort fields/directions. Defaults are createdAt,desc;
SQL-backed adapters add id as a stable tie-breaker. Search is case-insensitive and treats %, _ and ! literally.
Users support enabled/locked filters; roles support active; permissions support resource/action/active.
PageResult preserves the required content/page envelope and count/page metadata without Spring Data types.

## Business behavior

- Public registration accepts no role or permission input and assigns only the active default USER role.
  Admin creation can select validated roles, defaulting to USER when none are specified. System metadata
  remains protected; active system roles/permissions can participate in separate audited assignments.
- New passwords require at least 12 characters and no more than 72 UTF-8 bytes, matching the configured BCrypt
  encoder limit. Existing weak demo passwords remain valid until changed. Self-service changes check the
  current password; administrative resets do not. Hashing/checking uses PasswordHasher, never plaintext persistence.
- User enable/disable/lock/unlock/delete, profile/password updates and login-attempt recording preserve domain
  audit/version rules. Unknown login attempts are a no-op; successful attempts reset consecutive failures.
  No automatic lockout threshold was specified, so account locking remains an explicit use case.
- Assignments validate distinct bounded identifier batches, target eligibility, expiry and duplicate active
  assignments. A direct GRANT and DENY can coexist; DENY wins. Revoking a direct permission revokes all
  unrevoked effects for that user/permission. Revocations preserve rows and capture actor/time/reason.
- GetUserAuthoritiesUseCase and authentication use the domain effective-permission resolver, including
  expiry, revocation, disabled/deleted records and direct-denial precedence.
- Identity link/unlink acquires the owner-row lock within the write transaction. Another user's identity
  cannot be unlinked or transferred. The last sign-in method is protected. Explicit relinking of the same
  historical provider/subject restores its existing UUID/row, preserving the database's historical unique keys.

## Federation trust boundary

FederatedIdentityCommand must be built by an adapter only after validating the provider's OAuth/OIDC proof.
It is not an unauthenticated HTTP payload. The Step 8 infrastructure callback adapter constructs it only after Spring OAuth/OIDC proof validation; profile completion can supply only missing mandatory fields.

FindOrProvisionFederatedUserUseCase locates existing users by provider+subject, never by automatic email merging.
For a new account, missing required email/given/family names returns profile_completion_required with the missing
fields and performs no provisioning. It does not invent a verified email/name. Matching an existing email requires
signing in to that account and explicitly linking the provider, even if the provider claims the email is verified.

New social users have no password and receive USER only; provider role/permission attributes are ignored.
An available valid provider handle may become the username; otherwise an internal provider-prefixed UUID name is generated.
Existing provider logins refresh the identity's profile snapshot and login timestamps, without silently replacing the
internal account email/verification or profile. Disabled/deleted accounts and unlinked identities cannot sign in.
A rolled-back unique-key collision during concurrent first provisioning retries only the exact provider+subject;
tests verify both calls converge on one internal account. Relinking requires the explicit authenticated linking flow.

## Ports, transactions and security wiring

Required output ports UserRepository, RoleRepository and PermissionRepository are implemented in persistence;
PasswordHasher and CurrentActorProvider are implemented in infrastructure. Additional IdentityRepository,
AssignmentRepository and TransactionRunner ports support federation, history and framework-free atomic use cases.
The previous UserAuthenticationPort is a compatibility adapter that delegates to LoadUserForAuthenticationUseCase.

SpringTransactionRunner supplies read-only/read and REQUIRED/write boundaries. Multi-write use cases run atomically;
failed later identifiers roll back earlier inserts/assignments. Database uniqueness and optimistic-lock failures become
application conflict/concurrent_update codes without exposing SQL values. Owner/role pessimistic-lock methods require
an outer transaction. CurrentActorProvider supplies the internal local-user UUID, JWT subject, or explicit anonymous/external markers.

UserDetailsService now delegates to LoadUserForAuthenticationUseCase. Local authentication events invoke
RecordLoginAttemptUseCase; an audit persistence error is logged without leaking credentials or converting a successful
credential check into an unexplained sign-in failure. The listener is best-effort when persistence is unavailable.

The application's use cases are trusted internal operations. REST permission checks, current-user ownership routing,
JWT API security, HTTP DTO validation and RFC 9457 mapping are implemented in iam-rest-api (Step 6); the presence of
these Java beans alone does not expose or authorize public admin endpoints. The Step 8 federation adapter binds linking
callbacks to the authenticated internal owner and validates provider responses before creating the command.

## Verification

Run from iam-service: ./gradlew check bootJar. Isolated application tests use JUnit/Mockito (test dependencies only);
real H2/PostgreSQL integration tests cover all 39 use cases. Tests include privilege-safe registration, credentials,
PATCH behavior, account lifecycle, assignment histories/denials, SQL pagination/filtering, rollback of partial commands,
identity ownership/relinking, missing-provider-profile handling, UUID audit actors, form-login attempt recording,
session credential erasure and concurrent first provisioning. Existing migration/domain/browser tests remain included.

Verified: PostgreSQL 84 tests, zero failures/skips. H2: 81 passed, 1 PostgreSQL-only parameterized container skipped (82 reported).
IntelliJ compilation, module-boundary checks and bootJar passed; jdeps found only JDK/domain-core runtime dependencies. PostgreSQL tests use a disposable iam_test_... database; no live IAM database is modified.


## Browser support added in Step 9

GetConsentClientUseCase, ListConnectedAppsUseCase and RevokeConnectedAppUseCase delegate through the plain
ConnectedApplications port; all framework and SQL details remain in infrastructure. Browser callers supply the
canonical username of the authenticated internal user, never an owner from submitted fields. The consent view
loads registered metadata; native protocol state validation remains in Spring Security. Profile commands accept an
optional expectedVersion for stale-form protection; existing API callers retain the two-argument constructor.
