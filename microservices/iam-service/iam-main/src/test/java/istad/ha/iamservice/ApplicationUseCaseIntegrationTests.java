package istad.ha.iamservice;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.application.query.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.exception.DomainException;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationUseCaseIntegrationTests extends IamIntegrationTestSupport {
    static final String PASSWORD = "Str0ng!Pass2026";
    @Autowired ApplicationContext beans;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired IdentityRepository identities;
    @Autowired AssignmentRepository assignments;
    @Autowired PasswordHasher hasher;
    @Autowired MockMvc mvc;
    <T> T use(Class<T> type) { return beans.getBean(type); }
    String unique() { return UUID.randomUUID().toString().replace("-", ""); }
    UserResult register() { String id = unique(); return register("u" + id, id + "@example.com"); }
    UserResult register(String username, String email) {
        return use(RegisterUserUseCase.class).execute(new RegisterUserCommand(username, email, PASSWORD, PASSWORD, "Dara", "Sok", "en"));
    }
    PermissionResult permission() { return use(CreatePermissionUseCase.class).execute(new CreatePermissionCommand("test_" + unique(), "read", "Test")); }
    RoleResult role() { return use(CreateRoleUseCase.class).execute(new CreateRoleCommand("R" + unique(), "Test", List.of())); }
    FederatedIdentityCommand provider(Provider provider, String subject, String email) {
        return new FederatedIdentityCommand(provider, subject, email, email != null, "Dara Sok", "p" + unique(), "https://example.com/avatar",
                Map.of("id", subject, "roles", List.of("ADMIN")), email == null ? null : "Dara", email == null ? null : "Sok", "km");
    }
    void applicationCode(String code, org.junit.jupiter.api.function.Executable action) { assertEquals(code, assertThrows(ApplicationException.class, action).code()); }
    @Test void registrationAssignsOnlyUserAndExposesSafeResults() {
        var user = register(); var auth = use(LoadUserForAuthenticationUseCase.class).execute(user.email().toUpperCase());
        assertEquals(user.uuid(), auth.uuid()); assertTrue(auth.authorities().contains("ROLE_USER")); assertFalse(auth.authorities().contains("ROLE_ADMIN"));
        assertTrue(hasher.matches(PASSWORD, auth.passwordHash())); assertFalse(user.toString().contains(auth.passwordHash()));
        assertEquals(user.username(), use(GetUserUseCase.class).execute(user.uuid()).username());
        applicationCode("conflict", () -> register("u" + unique(), user.email().toUpperCase()));
    }
    @Test void administratorCreationAndAllUserAccountLifecycleUseCasesWork() {
        var permission = permission();
        var role = use(CreateRoleUseCase.class).execute(new CreateRoleCommand("R" + unique(), "Custom", List.of(permission.uuid())));
        String id = unique();
        var user = use(CreateUserUseCase.class).execute(new CreateUserCommand("u" + id, id + "@example.com", PASSWORD,
                UserProfile.basic("Dara", "Sok", "km"), List.of(role.uuid())));
        var auth = use(GetUserAuthoritiesUseCase.class).execute(user.uuid());
        assertTrue(auth.authorities().contains("ROLE_" + role.name())); assertTrue(auth.effectivePermissions().contains(permission.name()));
        assertFalse(auth.authorities().contains("ROLE_USER"));
        use(DisableUserUseCase.class).execute(user.uuid()); assertFalse(use(LoadUserForAuthenticationUseCase.class).execute(user.username()).enabled());
        use(EnableUserUseCase.class).execute(user.uuid()); use(LockUserUseCase.class).execute(user.uuid());
        assertFalse(use(LoadUserForAuthenticationUseCase.class).execute(user.username()).accountNonLocked());
        use(UnlockUserUseCase.class).execute(user.uuid()); assertTrue(use(LoadUserForAuthenticationUseCase.class).execute(user.username()).accountNonLocked());
        use(DeleteUserUseCase.class).execute(user.uuid()); applicationCode("not_found", () -> use(GetUserUseCase.class).execute(user.uuid()));
    }
    @Test void profilePatchPreservesOmittedFieldsAndSupportsExplicitClearing() {
        var user = register();
        var updated = use(UpdateUserProfileUseCase.class).execute(new UpdateUserProfileCommand(user.uuid(),
                ProfilePatch.builder().nickname("D").locale("km").phoneNumber("+85512345679").build()));
        assertEquals("Dara", updated.profile().givenName()); assertFalse(updated.profile().phoneNumberVerified());
        assertEquals("km", updated.profile().locale()); assertEquals("D", updated.profile().nickname());
        var cleared = use(UpdateUserProfileUseCase.class).execute(new UpdateUserProfileCommand(user.uuid(), ProfilePatch.builder().nickname(null).phoneNumber(null).build()));
        assertNull(cleared.profile().nickname()); assertNull(cleared.profile().phoneNumber()); assertEquals("km", cleared.profile().locale());
        var other = register();
        applicationCode("conflict", () -> use(UpdateUserProfileUseCase.class).execute(new UpdateUserProfileCommand(user.uuid(),
                ProfilePatch.builder().nickname("must-rollback").email(other.email()).build())));
        assertNull(use(GetUserUseCase.class).execute(user.uuid()).profile().nickname());
    }
    @Test void passwordChangeChecksCurrentPasswordAndAdminResetDoesNotRequireIt() {
        var user = register();
        applicationCode("invalid_current_password", () -> use(ChangePasswordUseCase.class).execute(new ChangePasswordCommand(user.uuid(), "wrong", "N3w!Pass2026!", "N3w!Pass2026!")));
        use(ChangePasswordUseCase.class).execute(new ChangePasswordCommand(user.uuid(), PASSWORD, "N3w!Pass2026!", "N3w!Pass2026!"));
        assertTrue(hasher.matches("N3w!Pass2026!", users.findByUuid(user.uuid()).orElseThrow().passwordHash()));
        use(ResetUserPasswordUseCase.class).execute(new ResetUserPasswordCommand(user.uuid(), "Reset!Pass2026", "Reset!Pass2026"));
        assertTrue(hasher.matches("Reset!Pass2026", users.findByUuid(user.uuid()).orElseThrow().passwordHash()));
        assertNotNull(use(GetUserUseCase.class).execute(user.uuid()).passwordChangedAt());
    }
    @Test void roleAndPermissionMetadataAndLifecycleUseCasesWork() {
        var permission = permission();
        var updated = use(UpdatePermissionUseCase.class).execute(new UpdatePermissionCommand(permission.uuid(), null, FieldUpdate.set("publish"), FieldUpdate.set(null)));
        assertEquals(permission.resource() + ":publish", updated.name()); assertNull(updated.description());
        assertEquals(updated.name(), use(GetPermissionUseCase.class).execute(permission.uuid()).name());
        use(DeactivatePermissionUseCase.class).execute(permission.uuid()); assertFalse(use(GetPermissionUseCase.class).execute(permission.uuid()).active());
        use(ActivatePermissionUseCase.class).execute(permission.uuid()); assertTrue(use(GetPermissionUseCase.class).execute(permission.uuid()).active());
        var role = role();
        var updatedRole = use(UpdateRoleUseCase.class).execute(new UpdateRoleCommand(role.uuid(), null, FieldUpdate.set("Updated")));
        assertEquals(role.name(), updatedRole.name()); assertEquals("Updated", use(GetRoleUseCase.class).execute(role.uuid()).description());
        use(DeactivateRoleUseCase.class).execute(role.uuid()); assertFalse(use(GetRoleUseCase.class).execute(role.uuid()).active());
        use(ActivateRoleUseCase.class).execute(role.uuid()); assertTrue(use(GetRoleUseCase.class).execute(role.uuid()).active());
        use(DeleteRoleUseCase.class).execute(role.uuid()); applicationCode("not_found", () -> use(GetRoleUseCase.class).execute(role.uuid()));
        use(DeletePermissionUseCase.class).execute(permission.uuid()); applicationCode("not_found", () -> use(GetPermissionUseCase.class).execute(permission.uuid()));
        var system = roles.findByName("ADMIN").orElseThrow();
        assertEquals("system_protected", assertThrows(DomainException.class, () -> use(DeleteRoleUseCase.class).execute(system.uuid())).code());
    }
    @Test void assignmentAndRevocationUseCasesPreserveAuditedHistoryAndDenyPrecedence() {
        var user = register(); var role = role(); var permission = permission();
        use(AssignPermissionsToRoleUseCase.class).execute(new AssignPermissionsToRoleCommand(role.uuid(), List.of(permission.uuid()), null, "Role grant"));
        var assigned = use(AssignRolesToUserUseCase.class).execute(new AssignRolesToUserCommand(user.uuid(), List.of(role.uuid()), Instant.now().plusSeconds(3600), "Temporary role"));
        assertEquals(role.uuid(), assigned.get(0).roleUuid());
        applicationCode("already_assigned", () -> use(AssignRolesToUserUseCase.class).execute(new AssignRolesToUserCommand(user.uuid(), List.of(role.uuid()), null, "Duplicate")));
        use(GrantPermissionsToUserUseCase.class).execute(new GrantPermissionsToUserCommand(user.uuid(), List.of(permission.uuid()), PermissionEffect.GRANT, null, "Direct grant"));
        use(GrantPermissionsToUserUseCase.class).execute(new GrantPermissionsToUserCommand(user.uuid(), List.of(permission.uuid()), PermissionEffect.DENY, null, "Direct denial"));
        assertFalse(use(GetUserAuthoritiesUseCase.class).execute(user.uuid()).effectivePermissions().contains(permission.name()));
        use(RevokePermissionFromUserUseCase.class).execute(user.uuid(), permission.uuid(), "Remove overrides");
        assertTrue(use(GetUserAuthoritiesUseCase.class).execute(user.uuid()).effectivePermissions().contains(permission.name()));
        var owner = users.findByUuid(user.uuid()).orElseThrow(); var rows = assignments.findUserPermissions(owner.id());
        assertEquals(2, rows.size()); assertTrue(rows.stream().allMatch(a -> a.metadata().revokedAt() != null));
        use(RevokePermissionFromRoleUseCase.class).execute(role.uuid(), permission.uuid(), "Remove role grant");
        assertFalse(use(GetUserAuthoritiesUseCase.class).execute(user.uuid()).effectivePermissions().contains(permission.name()));
        use(RevokeRoleFromUserUseCase.class).execute(user.uuid(), role.uuid(), "Remove membership");
        assertTrue(assignments.findUserRoles(owner.id()).stream().anyMatch(a -> a.id().equals(assigned.get(0).id()) && a.metadata().revokedAt() != null));
    }
    @Test void multiWriteCommandsRollBackWhenLaterIdentifiersAreInvalid() {
        var permission = permission(); String roleName = "R" + unique();
        applicationCode("not_found", () -> use(CreateRoleUseCase.class).execute(new CreateRoleCommand(roleName, "Rollback", List.of(permission.uuid(), UUID.randomUUID()))));
        assertTrue(roles.findByName(roleName).isEmpty());
        var user = register(); var role = role(); var owner = users.findByUuid(user.uuid()).orElseThrow();
        long original = assignments.findUserRoles(owner.id()).size();
        applicationCode("not_found", () -> use(AssignRolesToUserUseCase.class).execute(new AssignRolesToUserCommand(user.uuid(), List.of(role.uuid(), UUID.randomUUID()), null, "Rollback")));
        assertEquals(original, assignments.findUserRoles(owner.id()).size());
    }
    @Test void databasePagingFiltersSortsAndTreatsWildcardCharactersLiterally() {
        String prefix = "q" + unique(); register(prefix + "a", prefix + "a@example.com"); var b = register(prefix + "b", prefix + "b@example.com"); register(prefix + "c", prefix + "c@example.com");
        var page = use(ListUsersUseCase.class).execute(new UserFilterQuery(1, 1, "username,asc", prefix.toUpperCase(), null, null));
        assertEquals(3, page.page().totalElements()); assertEquals(3, page.page().totalPages()); assertEquals(b.username(), page.content().get(0).username());
        use(DisableUserUseCase.class).execute(b.uuid());
        assertEquals(1, use(ListUsersUseCase.class).execute(new UserFilterQuery(0, 20, null, prefix, false, null)).page().totalElements());
        String literal = "literal%_" + unique().substring(0, 10);
        var special = use(CreateRoleUseCase.class).execute(new CreateRoleCommand(literal, null, List.of()));
        assertEquals(List.of(special.uuid()), use(ListRolesUseCase.class).execute(new RoleFilterQuery(0, 20, "name,asc", "%_", true)).content().stream().map(RoleSummary::uuid).toList());
        var permission = permission();
        assertEquals(permission.uuid(), use(ListPermissionsUseCase.class).execute(new PermissionFilterQuery(0, 20, "name,asc", null, permission.resource(), "read", true)).content().get(0).uuid());
    }
    @Test void federationProvisionsExactlyOneUserWithoutTrustingProviderRoleAttributes() {
        String subject = unique(); var command = provider(Provider.GOOGLE, subject, unique() + "@example.com");
        var first = use(FindOrProvisionFederatedUserUseCase.class).execute(command);
        var second = use(FindOrProvisionFederatedUserUseCase.class).execute(command);
        assertEquals(first.uuid(), second.uuid()); assertNull(first.passwordHash()); assertTrue(first.emailVerified());
        assertTrue(first.authorities().contains("ROLE_USER")); assertFalse(first.authorities().contains("ROLE_ADMIN"));
        assertEquals(1, use(ListUserIdentitiesUseCase.class).execute(first.uuid()).size());
        assertNotNull(users.findByUuid(first.uuid()).orElseThrow().lastLoginAt());
        assertNotNull(identities.findByProviderAndSubject(Provider.GOOGLE, subject).orElseThrow().lastLoginAt());
    }
    @Test void federationNeverMergesEmailMatchesAndNeverInventsMissingProfiles() {
        var user = register();
        applicationCode("identity_link_required", () -> use(FindOrProvisionFederatedUserUseCase.class).execute(provider(Provider.GITHUB, unique(), user.email())));
        String subject = unique(); var command = provider(Provider.TELEGRAM, subject, null);
        applicationCode("profile_completion_required", () -> use(FindOrProvisionFederatedUserUseCase.class).execute(command));
        assertTrue(identities.findByProviderAndSubject(Provider.TELEGRAM, subject).isEmpty());
    }
    @Test void linkUnlinkAndRelinkEnforceOwnershipAndLastSignInProtection() {
        var google = provider(Provider.GOOGLE, unique(), unique() + "@example.com");
        var user = use(FindOrProvisionFederatedUserUseCase.class).execute(google);
        var googleIdentity = use(ListUserIdentitiesUseCase.class).execute(user.uuid()).get(0);
        var github = use(LinkIdentityUseCase.class).execute(user.uuid(), provider(Provider.GITHUB, unique(), null));
        use(UnlinkIdentityUseCase.class).execute(user.uuid(), googleIdentity.uuid());
        assertEquals("last_sign_in_method", assertThrows(DomainException.class, () -> use(UnlinkIdentityUseCase.class).execute(user.uuid(), github.uuid())).code());
        applicationCode("identity_unlinked", () -> use(FindOrProvisionFederatedUserUseCase.class).execute(google));
        var restored = use(LinkIdentityUseCase.class).execute(user.uuid(), google);
        assertEquals(googleIdentity.uuid(), restored.uuid()); assertEquals(2, use(ListUserIdentitiesUseCase.class).execute(user.uuid()).size());
        var other = register();
        applicationCode("identity_already_linked", () -> use(LinkIdentityUseCase.class).execute(other.uuid(), google));
        assertEquals("identity_not_found", assertThrows(DomainException.class, () -> use(UnlinkIdentityUseCase.class).execute(other.uuid(), restored.uuid())).code());
    }
    @Test void loginAttemptUseCaseAndFormEventsUpdateCountersAndUuidAudit() throws Exception {
        var user = register();
        mvc.perform(post("/login").with(csrf()).param("username", user.username()).param("password", "wrong")).andExpect(status().is3xxRedirection());
        assertEquals(1, users.findByUuid(user.uuid()).orElseThrow().failedLoginAttempts());
        var response = mvc.perform(post("/login").with(csrf()).param("username", user.email()).param("password", PASSWORD)).andExpect(status().is3xxRedirection()).andReturn();
        var security = (org.springframework.security.core.context.SecurityContext) response.getRequest().getSession()
                .getAttribute("SPRING_SECURITY_CONTEXT");
        assertNull(((IamUserPrincipal) security.getAuthentication().getPrincipal()).getPassword());
        var loaded = users.findByUuid(user.uuid()).orElseThrow(); assertEquals(0, loaded.failedLoginAttempts());
        assertNotNull(loaded.lastLoginAt()); assertEquals(user.uuid().toString(), loaded.audit().updatedBy());
        use(RecordLoginAttemptUseCase.class).execute("missing-" + unique(), false);
    }
    @Test void auditActorComesFromTheInternalUserUuid() {
        var user = register(); var principal = IamUserPrincipal.from(use(LoadUserForAuthenticationUseCase.class).execute(user.username()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var permission = permission();
            assertEquals(user.uuid().toString(), permissions.findByUuid(permission.uuid()).orElseThrow().audit().createdBy());
        } finally { SecurityContextHolder.clearContext(); }
    }
    @Test void concurrentFirstFederatedSignInsConvergeOnOneUser() throws Exception {
        var command = provider(Provider.FACEBOOK, unique(), unique() + "@example.com");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var first = pool.submit(() -> { release.await(); return use(FindOrProvisionFederatedUserUseCase.class).execute(command); });
            var second = pool.submit(() -> { release.await(); return use(FindOrProvisionFederatedUserUseCase.class).execute(command); });
            release.countDown();
            assertEquals(first.get(20, java.util.concurrent.TimeUnit.SECONDS).uuid(), second.get(20, java.util.concurrent.TimeUnit.SECONDS).uuid());
            var owner = users.findByUsernameOrEmail(command.email()).orElseThrow();
            assertEquals(1, identities.findByUserId(owner.id()).size());
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
