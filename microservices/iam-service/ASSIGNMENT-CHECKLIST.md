# IAM assignment requirements checklist

Source: III. Assignment - Spring Authorization Server & IAM Service.pdf, 14 pages.
Audited 6 November 2026. See DELIVERY-AUDIT.md for automated evidence and LIVE-PROVIDER-ACCEPTANCE.md for the remaining human account checks. Checked items record implementation/delivery evidence; they do not claim a grade or live-provider acceptance.
The page-by-page transcription below preserves field types, constraints, use-case signatures, endpoint authorities, payload examples, and claim lists.
The original PDF remains authoritative for visual formatting and Khmer text.

## Acceptance criteria

- [x] Objective: OAuth 2.1 / OIDC 1.0 IAM with User, Role and Permission as core domain; Authorization Server as adapter.
- [x] A: All seven modules and the exact dependency direction; domain plain Java; domain/application free of Authorization Server, JPA and web code.
- [x] B.1: Every users column, length, uniqueness/nullability rule, OIDC mapping and UserDetails account flag; lower-case email; nullable social-only password; UUID subject; E.164 phone, ISO birthdate/country, IANA zoneinfo; login/password timestamps and failed attempts.
- [x] B.2: Every roles column; UUID/name uniqueness; protected system roles; active flag; deletion/audit/version columns.
- [x] B.3: Every permissions column; resource:action naming; protected system permissions; active flag; deletion/audit/version columns.
- [x] B.4: Every identities column; provider and provider_subject compound uniqueness; raw JSONB attributes; linked/last-login timestamps; deletion/audit/version columns.
- [x] B.5: Separate user_roles, role_permissions and user_permissions entities, matching ERD foreign keys; direct user permission effect GRANT/DENY; assignment actor/time, expiry, revocation actor/time and reason. No @ManyToMany; assignments revoked, never deleted.
- [x] B.6: ROLE_ authorities and resource:action authorities; active role permissions plus direct GRANT minus direct DENY; protect system records; reject inactive assignments; one identity/provider/user; prohibit unlinking last sign-in method; seed ADMIN/USER and user/role/permission permissions.
- [x] C.1: Every named user use case with specified command/query/result; enable/disable and lock/unlock each separate; registration vs admin creation and password change vs admin reset remain distinct.
- [x] C.2: Every named role use case, including activation/deactivation and permission assignment/revocation.
- [x] C.3: Every named permission use case, including activation/deactivation.
- [x] C.4: FindOrProvisionFederatedUserUseCase, LinkIdentityUseCase, UnlinkIdentityUseCase and ListUserIdentitiesUseCase.
- [x] C.5: UserRepository, RoleRepository, PermissionRepository persistence ports; PasswordHasher and CurrentActorProvider infrastructure ports.
- [x] Persistence: Spring Data JPA adapters and Liquibase for all migrations.
- [x] D.1: All 23 user/auth/me REST method-route pairs, with exact authority and use-case mappings.
- [x] D.2: All 9 role REST method-route pairs, with exact authority and use-case mappings.
- [x] D.3: All 7 permission REST method-route pairs, with exact authority and use-case mappings.
- [x] D.4: All 8 supplied request examples: register, create user, patch profile, change password, assign roles, grant/deny permissions, create role, create permission. Match names and nesting; confirmation, expiry and reason fields included where specified.
- [x] D.5: All supplied response shapes: user, authorities, identity list, page envelope and RFC 9457 validation Problem Details with field errors. Never expose password hashes.
- [x] API security: own JWT Bearer tokens, public registration exception, authenticated self-service and permission-based admin operations; lists accept page/size/sort.
- [x] E.1: Explicit JdbcRegisteredClientRepository, JdbcOAuth2AuthorizationService and JdbcOAuth2AuthorizationConsentService; UserDetailsService delegates to LoadUserForAuthenticationUseCase; access token includes roles and permissions.
- [x] E.2: Scope-controlled openid/profile/email/phone/address claims, including all listed profile fields and users.uuid as sub.
- [x] E.3: POST /connect/register with initial access token from seeded client with client.create scope; specified metadata, 201 response, registration_access_token and registration_client_uri; GET registered client using registration access token.
- [x] E.4: Google/google, GitHub/github, Facebook/facebook and Telegram/telegram login through internal provisioning use case.
- [x] F: GET /login -> login.jte, social buttons and language switcher.
- [x] F: GET/POST /register -> register.jte, validation errors.
- [x] F: GET /oauth2/consent -> consent.jte, client name/scopes and Allow/Cancel.
- [x] F: GET / -> index.jte, profile claims/roles/permissions.
- [x] F: GET/POST /profile/edit -> profile-edit.jte.
- [x] F: GET/POST /profile/password -> change-password.jte.
- [x] F: GET /profile/accounts -> linked-accounts.jte, link/unlink social accounts.
- [x] F: GET /profile/apps -> connected-apps.jte, authorized apps and consent revocation.
- [x] F: messages.properties English and messages_km.properties Khmer; ?lang=en|km; no hard-coded template text; supplied eight message keys and formatted provider name.
- [x] Quality: one use case per class; controllers map DTOs and call use cases; soft deletion/auditing/optimistic locking everywhere; secrets only from environment variables.
- [x] Delivery: unit tests, integration tests, README, Postman collection and clear explanation of code/best practices.
- [ ] Grading: 15 + 15 + 15 + 20 + 15 implementation points; 20 explanation/quality points; total 100. Deadline 10 November 2026 11:59 AM; late penalty 25% of score.

## Ambiguities to resolve explicitly

- Required user email/given/family names versus missing social-provider attributes: document profile-completion behavior; do not invent verified emails.
- Broad audit/version-everywhere instruction versus smaller assignment-table column list: preserve assignment history and document any added audit/version columns.
- System records cannot be changed: clarify whether membership/permission assignments on system roles are permitted independently from role metadata changes.
- Several use cases/actions lack explicit REST payloads/routes: admin password reset, revocation reasons, identity linking and connected-app consent revocation. Implement the required behavior and document supplementary contracts.
- UserResult example name is given-name then family-name, despite the example username suggesting a different ordering; follow the explicit name rule.
- Khmer PDF text extraction is imperfect: use the visible PDF and verify translations, not extracted glyph order.
- Resolved during the final audit: the user delegated the choice; environment-supplied secrets were selected, with current workstation values preserved in protected Git-ignored local configuration. No stored secrets were rotated.

## Full source transcription by page

### Page 1

```text
INSTITUTE OF SCIENCE AND TECHNOLOGY
ADVANCED DEVELOPMENT

                                                             ✍️ ASSIGNMENT & RESEARCH |
                             IMPLEMENT SPRING AUTHORIZATION SERVER & IAM SERVICE |


  OBJECTIVE
Build iam-service, an Identity and Access Management service on Spring Authorization Server (OAuth 2.1
/ OpenID Connect 1.0). User, Role and Permission are the core domain, exposed through single-
responsibility use cases; Spring Authorization Server is an adapter. The service supports Dynamic Client
Registration, social login, a secured REST API and JTE pages in English and Khmer.

  IMPLEMENTATION WORKFLOW (+80 pts)
   ●   Part 1: Project Structure & Core Domain (+15 pts)
          ○ Create the multi-module project in Section A and respect its dependency rules.
          ○ Implement User, Role, Permission, Identity and the three audited assignment models in iam-
               domain-core following Section B.
   ●   Part 2: Use Cases & Persistence (+15 pts)
          ○ Define the use cases and output ports in Section C inside iam-application-service.
          ○ Implement the ports in iam-persistence with Spring Data JPA; map every assignment table
               as its own entity (no @ManyToMany).
          ○ Use Liquibase for all database migrations.
   ●   Part 3: REST API (+15 pts)
          ○ Expose the use cases through the endpoints and payloads in Section D inside iam-rest-
               api.
          ○ Secure the API with your own JWT access tokens and permission authorities; return errors
               as RFC 9457 Problem Details.
   ●   Part 4: Spring Authorization Server Adapter (+20 pts)
          ○ Configure JdbcRegisteredClientRepository, JdbcOAuth2AuthorizationService and
               JdbcOAuth2AuthorizationConsentService in iam-infrastructure.
          ○ Map OIDC claims, add roles and permissions to the access token and enable Dynamic
               Client Registration (Section E).
          ○ Add social login with Google, GitHub, Facebook and Telegram through
               FindOrProvisionFederatedUserUseCase.
   ●   Part 5: JTE Web Pages & Internationalization (+15 pts)
          ○ Implement the pages in Section F inside iam-web with JTE, in English (en) and Khmer (km).




  CLEARLY EXPLAIN AND BEST PRACTICES CODE (+20pts)
   ●   Domain and application modules stay free of Spring Authorization Server, JPA and web code.
   ●   One use case = one class; controllers only map DTOs and call use cases.
```

### Page 2

```text
●   Soft delete, auditing and optimistic locking everywhere; secrets only from environment variables.
   ●   Unit and integration tests, a README and a Postman collection.

☠️ DEADLINE -> 10-November-2026, 11:59 AM (Late submissions will be -25% of the score)
  TOTAL SCORE -> 100 pts
```

### Page 3

```text
SPECIFICATION

A. PROJECT STRUCTURE

                                                                                                         Modules     TREE

  1   iam-service/
  2   ├── iam-domain/
  3   │   ├── iam-domain-core/               User, Role, Permission, Identity, assignments
  4   │   └── iam-application-service/       use cases and ports
  5   ├── iam-persistence/                   JPA adapters
  6   ├── iam-infrastructure/                Spring Authorization Server adapter
  7   ├── iam-rest-api/                      REST API
  8   ├── iam-web/                           JTE pages and i18n
  9   └── iam-main/                          Spring Boot application



Module                    Responsibility                                                   Depends on
iam-domain-core           Domain models and rules. Plain Java.                             —
iam-application-service   Use cases, commands, results and output ports. No Spring         iam-domain-core
                          Authorization Server.
iam-persistence           JPA entities, repositories and port adapters.                    iam-application-service
iam-infrastructure        Spring Authorization Server, JDBC services, OIDC claims, token   iam-application-service
                          customizer, DCR, social login.
iam-rest-api              Controllers, DTOs, validation, API security.                     iam-application-service
iam-web                   JTE templates, web controllers, i18n.                            iam-application-service
iam-main                  Application entry point, wiring and configuration.               all modules




B. CORE DOMAIN & ERD
```

### Page 4

```text
User ↔ Role, Role ↔ Permission and User ↔ Permission are many-to-many through audited tables.
Assignments are revoked, never deleted.




                                   PK primary key FK foreign key UK unique

B.1 users OIDC Standard Claims + Spring Security UserDetails
Column                   Type           Constraint     Standard / Description
```

### Page 5

```text
id                        BIGINT         PK         Internal key
uuid                      UUID           UK, NOT    Public id; OIDC sub
                                         NULL
username                  VARCHAR(50)    UK, NOT    UserDetails getUsername(); OIDC preferred_username
                                         NULL
email                     VARCHAR(255)   UK, NOT    OIDC email (stored lower-case)
                                         NULL
email_verified            BOOLEAN        NOT NULL   OIDC email_verified
password_hash             VARCHAR(255)              UserDetails password hash; null for social-only users

given_name                VARCHAR(100)   NOT NULL   OIDC given_name
family_name               VARCHAR(100)   NOT NULL   OIDC family_name (name = given + family)
middle_name               VARCHAR(100)              OIDC middle_name

nickname                  VARCHAR(100)              OIDC nickname

gender                    VARCHAR(20)               OIDC gender (female, male, other)

birthdate                 DATE                      OIDC birthdate (ISO 8601 YYYY-MM-DD)

profile_url               VARCHAR(500)              OIDC profile

picture_url               VARCHAR(500)              OIDC picture

website_url               VARCHAR(500)              OIDC website

zoneinfo                  VARCHAR(50)               OIDC zoneinfo (IANA, e.g. Asia/Phnom_Penh)

locale                    VARCHAR(10)    NOT NULL   OIDC locale (en, km)
phone_number              VARCHAR(20)    UK         OIDC phone_number (E.164, e.g. +85512345678)
phone_number_verified     BOOLEAN        NOT NULL   OIDC phone_number_verified
address_street_address    VARCHAR(255)              OIDC address.street_address

address_locality          VARCHAR(100)              OIDC address.locality (city / khan)

address_region            VARCHAR(100)              OIDC address.region (province)

address_postal_code       VARCHAR(20)               OIDC address.postal_code

address_country           CHAR(2)                   OIDC address.country (ISO 3166-1, e.g. KH)

enabled                   BOOLEAN        NOT NULL   UserDetails isEnabled()
account_non_expired       BOOLEAN        NOT NULL   UserDetails isAccountNonExpired()
account_non_locked        BOOLEAN        NOT NULL   UserDetails isAccountNonLocked()
credentials_non_expired   BOOLEAN        NOT NULL   UserDetails isCredentialsNonExpired()
failed_login_attempts     INT            NOT NULL   Consecutive failed logins
last_login_at             TIMESTAMPTZ               Last successful sign-in

password_changed_at       TIMESTAMPTZ               Last password change

is_deleted                BOOLEAN        NOT NULL   Soft delete
deleted_at                TIMESTAMPTZ               When the row was soft-deleted

deleted_by                VARCHAR(100)              Actor sub

created_at                TIMESTAMPTZ    NOT NULL   Audit
created_by                VARCHAR(100)   NOT NULL   Audit
updated_at                TIMESTAMPTZ    NOT NULL   Audit; OIDC updated_at
updated_by                VARCHAR(100)   NOT NULL   Audit
version                   BIGINT         NOT NULL   Optimistic lock



B.2 roles
```

### Page 6

```text
Column                    Type           Constraint   Standard / Description
id                        BIGINT         PK           Internal key
uuid                      UUID           UK, NOT      Public identifier
                                         NULL
name                      VARCHAR(50)    UK, NOT      e.g. ADMIN
                                         NULL
description               VARCHAR(255)                Human-readable purpose of the role

is_system                 BOOLEAN        NOT NULL     Built-in, protected
is_active                 BOOLEAN        NOT NULL     Active flag
is_deleted                BOOLEAN        NOT NULL     Soft delete
deleted_at                TIMESTAMPTZ                 When the row was soft-deleted

deleted_by                VARCHAR(100)                Actor sub

created_at                TIMESTAMPTZ    NOT NULL     Audit
created_by                VARCHAR(100)   NOT NULL     Audit
updated_at                TIMESTAMPTZ    NOT NULL     Audit
updated_by                VARCHAR(100)   NOT NULL     Audit
version                   BIGINT         NOT NULL     Optimistic lock



B.3 permissions
Column                    Type           Constraint   Standard / Description
id                        BIGINT         PK           Internal key
uuid                      UUID           UK, NOT      Public identifier
                                         NULL
name                      VARCHAR(100)   UK, NOT      resource:action, e.g. user:read
                                         NULL
resource                  VARCHAR(50)    NOT NULL     Protected resource, e.g. user, role, order
action                    VARCHAR(50)    NOT NULL     Action, e.g. create, read, update, delete
description               VARCHAR(255)                Human-readable meaning

is_system                 BOOLEAN        NOT NULL     Built-in, protected
is_active                 BOOLEAN        NOT NULL     Active flag
is_deleted                BOOLEAN        NOT NULL     Soft delete
deleted_at                TIMESTAMPTZ                 When the row was soft-deleted

deleted_by                VARCHAR(100)                Actor sub

created_at                TIMESTAMPTZ    NOT NULL     Audit
created_by                VARCHAR(100)   NOT NULL     Audit
updated_at                TIMESTAMPTZ    NOT NULL     Audit
updated_by                VARCHAR(100)   NOT NULL     Audit
version                   BIGINT         NOT NULL     Optimistic lock




B.4 identities linked social accounts
Column                    Type           Constraint   Standard / Description
id                        BIGINT         PK           Internal key
uuid                      UUID           UK, NOT      Public identifier
                                         NULL
```

### Page 7

```text
user_id                        BIGINT          FK, NOT NULL       Owner → users.id
provider                       VARCHAR(20)     NOT NULL           GOOGLE, GITHUB, FACEBOOK, TELEGRAM

provider_subject               VARCHAR(255)    UK, NOT            Provider user id; unique with provider
                                               NULL
email                          VARCHAR(255)                       Email returned by the provider

email_verified                 BOOLEAN         NOT NULL           Whether the provider verified the email
display_name                   VARCHAR(150)                       Name returned by the provider

username                       VARCHAR(100)                       Provider handle (GitHub login, Telegram username)

picture_url                    VARCHAR(500)                       Avatar returned by the provider

attributes                     JSONB                              Raw provider attributes

linked_at                      TIMESTAMPTZ     NOT NULL           When the identity was linked to the user
last_login_at                  TIMESTAMPTZ                        Last sign-in through this provider

is_deleted                     BOOLEAN         NOT NULL           Soft delete
deleted_at                     TIMESTAMPTZ                        When the row was soft-deleted

deleted_by                     VARCHAR(100)                       Actor sub

created_at                     TIMESTAMPTZ     NOT NULL           Audit
created_by                     VARCHAR(100)    NOT NULL           Audit
updated_at                     TIMESTAMPTZ     NOT NULL           Audit
updated_by                     VARCHAR(100)    NOT NULL           Audit
version                        BIGINT          NOT NULL           Optimistic lock



B.5 Assignment tables
Column                Type               user_roles   role_perm     user_perm       Description
                                                      s             s
id                    BIGINT                                                        Primary key
user_id               BIGINT                                                        FK → users.id

role_id               BIGINT                                                        FK → roles.id

permission_id         BIGINT                                                        FK → permissions.id

effect                VARCHAR(5)                                                    GRANT or DENY

assigned_at           TIMESTAMPTZ                                                   Assigned time
assigned_by           VARCHAR(100)                                                  Actor sub
expires_at            TIMESTAMPTZ                                                   Optional expiry
revoked_at            TIMESTAMPTZ                                                   Null = active
revoked_by            VARCHAR(100)                                                  Actor sub
reason                VARCHAR(255)                                                  Why




B.6 Domain rules
     ●     Authorities: ROLE_ + role name, and permission name (resource:action).
     ●     Effective permissions = active role permissions + direct GRANT − direct DENY.
     ●     System roles/permissions cannot be changed or deleted; inactive ones cannot be assigned.
```

### Page 8

```text
●   One identity per provider per user; a user cannot unlink their last sign-in method.
    ●   Seed roles ADMIN and USER with permissions for user, role and permission.

C. USE CASES

C.1 User
Use case                           Input                              Output
RegisterUserUseCase                RegisterUserCommand                UserResult
CreateUserUseCase                  CreateUserCommand                  UserResult
GetUserUseCase                     userUuid                           UserResult
ListUsersUseCase                   UserFilterQuery                    PageResult<UserSummary>
UpdateUserProfileUseCase           UpdateUserProfileCommand           UserResult
ChangePasswordUseCase              ChangePasswordCommand              void
ResetUserPasswordUseCase           ResetUserPasswordCommand           void
EnableUserUseCase /                userUuid                           void
DisableUserUseCase
LockUserUseCase /                  userUuid                           void
UnlockUserUseCase
DeleteUserUseCase                  userUuid                           void
AssignRolesToUserUseCase           AssignRolesToUserCommand           List<RoleAssignmentResult>
RevokeRoleFromUserUseCase          userUuid, roleUuid, reason         void
GrantPermissionsToUserUseCase      GrantPermissionsToUserCommand      List<PermissionGrantResult>
RevokePermissionFromUserUseCase    userUuid, permissionUuid, reason   void
GetUserAuthoritiesUseCase          userUuid                           UserAuthoritiesResult
LoadUserForAuthenticationUseCase   usernameOrEmail                    AuthUserResult
RecordLoginAttemptUseCase          usernameOrEmail, success           void



C.2 Role
Use case                           Input                              Output
CreateRoleUseCase                  CreateRoleCommand                  RoleResult
GetRoleUseCase                     roleUuid                           RoleResult
ListRolesUseCase                   RoleFilterQuery                    PageResult<RoleSummary>
UpdateRoleUseCase                  UpdateRoleCommand                  RoleResult
ActivateRoleUseCase /              roleUuid                           void
DeactivateRoleUseCase
DeleteRoleUseCase                  roleUuid                           void
AssignPermissionsToRoleUseCase     AssignPermissionsToRoleCommand     List<PermissionAssignmentResult>
RevokePermissionFromRoleUseCase    roleUuid, permissionUuid, reason   void




C.3 Permission
Use case                           Input                              Output
CreatePermissionUseCase            CreatePermissionCommand            PermissionResult
GetPermissionUseCase               permissionUuid                     PermissionResult
ListPermissionsUseCase             PermissionFilterQuery              PageResult<PermissionResult>
UpdatePermissionUseCase            UpdatePermissionCommand            PermissionResult
```

### Page 9

```text
ActivatePermissionUseCase /            permissionUuid                                void
DeactivatePermissionUseCase
DeletePermissionUseCase                permissionUuid                                void



C.4 Identity (user federation)
Use case                               Input                                         Output
FindOrProvisionFederatedUserUseCa      FederatedIdentityCommand                      AuthUserResult
se
LinkIdentityUseCase                    userUuid, FederatedIdentityCommand            IdentityResult
UnlinkIdentityUseCase                  userUuid, identityUuid                        void
ListUserIdentitiesUseCase              userUuid                                      List<IdentityResult>



C.5 Output ports
Port                                                                Implemented in
UserRepository                                                      iam-persistence
RoleRepository                                                      iam-persistence
PermissionRepository                                                iam-persistence
PasswordHasher                                                      iam-infrastructure
CurrentActorProvider                                                iam-infrastructure



D. REST API
Base path /api, Bearer token required. Lists accept page, size, sort.

D.1 Users
Method     Endpoint                                             Use case                                    Authority
 POST      /api/auth/register                                   RegisterUserUseCase                         public
 POST      /api/users                                           CreateUserUseCase                           user:create
 GET       /api/users                                           ListUsersUseCase                            user:read
 GET       /api/users/{uuid}                                    GetUserUseCase                              user:read
 PATCH     /api/users/{uuid}                                    UpdateUserProfileUseCase                    user:update
 DELETE    /api/users/{uuid}                                    DeleteUserUseCase                           user:delete
 POST      /api/users/{uuid}/enable                             EnableUserUseCase                           user:update
 POST      /api/users/{uuid}/disable                            DisableUserUseCase                          user:update
 POST      /api/users/{uuid}/lock                               LockUserUseCase                             user:update
 POST      /api/users/{uuid}/unlock                             UnlockUserUseCase                           user:update
 PUT       /api/users/{uuid}/password                           ResetUserPasswordUseCase                    user:update
 GET       /api/users/{uuid}/authorities                        GetUserAuthoritiesUseCase                   user:read
 POST      /api/users/{uuid}/roles                              AssignRolesToUserUseCase                    user:assign
 DELETE    /api/users/{uuid}/roles/{roleUuid}                   RevokeRoleFromUserUseCase                   user:assign
 POST      /api/users/{uuid}/permissions                        GrantPermissionsToUserUseCase               user:assign
 DELETE    /api/users/{uuid}/permissions/{permissionU           RevokePermissionFromUserUseCase             user:assign
           uid}
 GET       /api/users/{uuid}/identities                         ListUserIdentitiesUseCase                   user:read
 DELETE    /api/users/{uuid}/identities/{identityUuid           UnlinkIdentityUseCase                       user:update
           }
 GET       /api/me                                              GetUserUseCase                              authenticated
 PATCH     /api/me                                              UpdateUserProfileUseCase                    authenticated
```

### Page 10

```text
PUT          /api/me/password                             ChangePasswordUseCase             authenticated
GET          /api/me/identities                           ListUserIdentitiesUseCase         authenticated
DELETE       /api/me/identities/{identityUuid}            UnlinkIdentityUseCase             authenticated




D.2 Roles
Method       Endpoint                                     Use case                          Authority
POST         /api/roles                                   CreateRoleUseCase                 role:create
GET          /api/roles                                   ListRolesUseCase                  role:read
GET          /api/roles/{uuid}                            GetRoleUseCase                    role:read
PATCH        /api/roles/{uuid}                            UpdateRoleUseCase                 role:update
DELETE       /api/roles/{uuid}                            DeleteRoleUseCase                 role:delete
POST         /api/roles/{uuid}/activate                   ActivateRoleUseCase               role:update
POST         /api/roles/{uuid}/deactivate                 DeactivateRoleUseCase             role:update
POST         /api/roles/{uuid}/permissions                AssignPermissionsToRoleUseCase    role:assign
DELETE       /api/roles/{uuid}/permissions/{permissionU   RevokePermissionFromRoleUseCase   role:assign
             uid}




D.3 Permissions
Method       Endpoint                                     Use case                          Authority
POST         /api/permissions                             CreatePermissionUseCase           permission:create
GET          /api/permissions                             ListPermissionsUseCase            permission:read
GET          /api/permissions/{uuid}                      GetPermissionUseCase              permission:read
PATCH        /api/permissions/{uuid}                      UpdatePermissionUseCase           permission:update
DELETE       /api/permissions/{uuid}                      DeletePermissionUseCase           permission:delete
POST         /api/permissions/{uuid}/activate             ActivatePermissionUseCase         permission:update
POST         /api/permissions/{uuid}/deactivate           DeactivatePermissionUseCase       permission:update




D.4 Request payloads

POST     /api/auth/register                                                                 RegisterUserRequest   JSON

  1     {
  2         "username": "sokdara",
  3         "email": "dara.sok@istad.co",
  4         "password": "Str0ng!Pass2026",
  5         "confirmPassword": "Str0ng!Pass2026",
  6         "givenName": "Dara",
  7         "familyName": "Sok",
  8         "locale": "km"
  9     }



POST     /api/users                                                                          CreateUserRequest    JSON

  1     {
  2         "username": "sokdara",
  3         "email": "dara.sok@istad.co",
  4         "password": "Str0ng!Pass2026",
  5         "givenName": "Dara",
  6         "familyName": "Sok",
  7         "gender": "male",
  8         "birthdate": "2003-05-20",
  9         "phoneNumber": "+85512345678",
 10         "zoneinfo": "Asia/Phnom_Penh",
 11         "locale": "km",
```

### Page 11

```text
12         "address": {
 13           "streetAddress": "No. 24, St. 562",
 14           "locality": "Toul Kork",
 15           "region": "Phnom Penh",
 16           "postalCode": "120408",
 17           "country": "KH"
 18         },
 19         "roleUuids": ["0b9e6c1a-6a57-4a8e-9e0b-3a0f4d6f2c11"]
 20     }



PATCH       /api/users/{uuid}   ·   /api/me                                     UpdateUserProfileRequest   JSON

 1      {
 2          "givenName": "Dara",
 3          "nickname": "Dara",
 4          "phoneNumber": "+85512345678",
 5          "locale": "en"
 6      }



PUT     /api/me/password                                                         ChangePasswordRequest     JSON

 1      {
 2          "currentPassword": "Str0ng!Pass2026",
 3          "newPassword": "N3w!Pass2026",
 4          "confirmPassword": "N3w!Pass2026"
 5      }



POST    /api/users/{uuid}/roles                                                AssignRolesToUserRequest    JSON

 1      {
 2          "roleUuids": ["0b9e6c1a-6a57-4a8e-9e0b-3a0f4d6f2c11"],
 3          "expiresAt": "2026-12-31T23:59:59Z",
 4          "reason": "Temporary admin"
 5      }



POST    /api/users/{uuid}/permissions                                      GrantPermissionsToUserRequest   JSON

 1      {
 2          "permissionUuids": ["5d1c2f0e-2b8a-4c3e-9f61-7e2d9a0b4c55"],
 3          "effect": "GRANT",
 4          "reason": "Needs to read reports"
 5      }



POST    /api/roles                                                                    CreateRoleRequest    JSON

 1      {
 2          "name": "CONTENT_MANAGER",
 3          "description": "Manages course content",
 4          "permissionUuids": ["5d1c2f0e-2b8a-4c3e-9f61-7e2d9a0b4c55"]
 5      }



POST    /api/permissions                                                         CreatePermissionRequest   JSON

 1      {
 2          "resource": "course",
 3          "action": "publish",
 4          "description": "Publish a course"
 5      }



D.5 Response payloads
```

### Page 12

```text
GET   /api/users/{uuid}                                                                               UserResponse     JSON

  1    {
  2        "uuid": "7f3d2a10-91c4-4b0e-8d6f-2a9c5e1b7d40",
  3        "username": "sokdara",
  4        "email": "dara.sok@istad.co",
  5        "emailVerified": false,
  6        "name": "Dara Sok",
  7        "locale": "km",
  8        "enabled": true,
  9        "roles": [ { "uuid": "0b9e6c1a-6a57-4a8e-9e0b-3a0f4d6f2c11", "name": "USER" } ],
 10        "createdAt": "2026-11-05T02:15:00Z",
 11        "updatedAt": "2026-11-05T02:15:00Z"
 12    }



 GET   /api/users/{uuid}/authorities                                                          UserAuthoritiesResponse   JSON

  1    {
  2      "roles": [ { "uuid": "0b9e6c1a-6a57-4a8e-9e0b-3a0f4d6f2c11", "name": "ADMIN", "expiresAt": "2026-12-
       31T23:59:59Z" } ],
  3      "directPermissions": [ { "name": "report:read", "effect": "GRANT" } ],
  4      "effectivePermissions": ["user:read", "user:update", "report:read"]
  5    }



 GET   /api/me/identities                                                                      List<IdentityResponse>   JSON

  1    [
  2        {
  3            "uuid": "c4e8a2d1-7b3f-4e6a-9d20-5f1b8c3a7e90",
  4            "provider": "GOOGLE",
  5            "email": "dara.sok@gmail.com",
  6            "displayName": "Dara Sok",
  7            "linkedAt": "2026-11-05T02:30:00Z"
  8        }
  9    ]



 GET   /api/roles                                                                                      PageResponse     JSON

  1    {
  2        "content": [ { "uuid": "0b9e6c1a-6a57-4a8e-9e0b-3a0f4d6f2c11", "name": "ADMIN", "active": true } ],
  3        "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
  4    }



                                                                                         ProblemDetail (RFC 9457)       ERROR

  1    {
  2        "type": "https://iam.istad.co/problems/validation-error",
  3        "title": "Validation failed",
  4        "status": 400,
  5        "errors": [ { "field": "email", "message": "must be a well-formed email address" } ]
  6    }



E. SPRING AUTHORIZATION SERVER

E.1 Adapter components
Component                                        Implementation
RegisteredClientRepository                       JdbcRegisteredClientRepository

OAuth2AuthorizationService                       JdbcOAuth2AuthorizationService

OAuth2AuthorizationConsentService                JdbcOAuth2AuthorizationConsentService

UserDetailsService                               Adapter → LoadUserForAuthenticationUseCase
```

### Page 13

```text
Token customizer                                       Adds roles and permissions to the access token



E.2 Scopes & claims
Scope                  Claims
openid                 sub (= users.uuid)

profile                name, given_name, family_name, middle_name, nickname, preferred_username, profile, picture, website, gender,
                       birthdate, zoneinfo, locale, updated_at

email                  email, email_verified

phone                  phone_number, phone_number_verified

address                address




E.3 Dynamic Client Registration

                                                                                                                Register client   HTTP

  1      POST /connect/register HTTP/1.1
  2      Authorization: Bearer <initial_access_token>
  3      Content-Type: application/json
  4
  5      {
  6          "client_name": "ISTAD E-Commerce Web",
  7          "redirect_uris": ["http://localhost:8081/login/oauth2/code/iam"],
  8          "grant_types": ["authorization_code", "refresh_token"],
  9          "response_types": ["code"],
 10          "scope": "openid profile email",
 11          "token_endpoint_auth_method": "client_secret_basic"
 12      }



                                                                                                                    Response      HTTP

  1      HTTP/1.1 201 Created
  2      Content-Type: application/json
  3
  4      {
  5          "client_id": "Q2xpZW50LTE3MzA3",
  6          "client_secret": "uN7vS9...",
  7          "client_name": "ISTAD E-Commerce Web",
  8          "redirect_uris": ["http://localhost:8081/login/oauth2/code/iam"],
  9          "registration_access_token": "eyJraWQiOi...",
 10          "registration_client_uri": "http://localhost:9000/connect/register?client_id=Q2xpZW50LTE3MzA3"
 11      }



The initial access token comes from a seeded client with scope client.create. Read a client with GET
/connect/register?client_id=... and its registration_access_token.

E.4 Social login
Provider                                                             Registration id
Google                                                               google

GitHub                                                               github

Facebook                                                             facebook

Telegram                                                             telegram



F. JTE WEB PAGES & INTERNATIONALIZATION
Route                                 Template                        Description
```

### Page 14

```text
GET /login                    login.jte             Sign in, social login buttons, language switcher.
GET, POST /register           register.jte          Sign-up form with validation errors.
GET /oauth2/consent           consent.jte           Client name, requested scopes, Allow / Cancel.
GET /                         index.jte             Profile: claims, roles and permissions.
GET, POST /profile/edit       profile-edit.jte      Edit profile.
GET, POST /profile/password   change-password.jte   Change password.
GET /profile/accounts         linked-accounts.jte   Link and unlink social accounts.
GET /profile/apps             connected-apps.jte    Authorized apps; revoke consent.


Bundles: messages.properties (en) and messages_km.properties (km). Switch language with ?lang=en|km;
no hard-coded text in templates.
Key                            en                                      km
login.title                    Sign in                                 ចូលគណនី

login.continueWith             Continue with {0}                       បន មួយ {0}

register.title                 Create an account                       បេងើតគណនី

consent.allow                  Allow                                   អនុ ាត

consent.cancel                 Cancel                                  េ ះបង់

profile.title                  My profile                              បវតិរូបរបស់ខំ

profile.linkedAccounts         Linked accounts                         គណនីែដល ន ប់

common.logout                  Sign out                                  កេចញ
```

