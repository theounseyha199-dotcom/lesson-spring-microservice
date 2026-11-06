package istad.ha.iamservice.application;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.application.query.*;
import istad.ha.iamservice.application.support.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationUseCaseTests {
    final Instant at = Instant.parse("2026-11-06T00:00:00Z");
    final UserRepository users = mock(UserRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final PermissionRepository permissions = mock(PermissionRepository.class);
    final IdentityRepository identities = mock(IdentityRepository.class);
    final AssignmentRepository assignments = mock(AssignmentRepository.class);
    final PasswordHasher passwords = mock(PasswordHasher.class);
    int reads; int writes;
    final TransactionRunner transactions = new TransactionRunner() {
        public <T> T read(java.util.function.Supplier<T> operation) { reads++; return operation.get(); }
        public <T> T write(java.util.function.Supplier<T> operation) { writes++; return operation.get(); }
    };
    final UseCaseContext context = new UseCaseContext(users, roles, permissions, identities, assignments, passwords,
            () -> "actor-sub", transactions, Clock.fixed(at, ZoneOffset.UTC));
    User user() {
        return new User(1L, UUID.randomUUID(), "dara", "dara@example.com", true, "sensitive-hash", UserProfile.basic("Dara", "Sok", "en"),
                true, true, true, true, 0, null, null, SoftDeletion.active(), AuditMetadata.create("creator", at.minusSeconds(1)));
    }
    @Test void registrationRejectsConfirmationBeforeWritingOrHashing() {
        var command = new RegisterUserCommand("dara", "dara@example.com", "Str0ng!Pass2026", "different", "Dara", "Sok", "en");
        assertEquals("validation", assertThrows(ApplicationException.class, () -> new RegisterUserUseCase(context).execute(command)).code());
        verifyNoInteractions(users, assignments, passwords);
        assertEquals(1, writes);
    }
    @Test void getResultRedactsAuthenticationFieldsAndUsesReadBoundary() {
        var user = user(); when(users.findByUuid(user.uuid())).thenReturn(Optional.of(user));
        var result = new GetUserUseCase(context).execute(user.uuid());
        assertEquals(user.uuid(), result.uuid()); assertFalse(result.toString().contains("sensitive-hash"));
        assertEquals(1, reads); assertEquals(0, writes);
    }
    @Test void profilePatchDistinguishesOmissionClearingAndVerificationChanges() {
        var user = user();
        var changed = ProfilePatch.builder().nickname("D").email("NEW@EXAMPLE.COM").build().apply(user, "actor", at);
        assertEquals("Dara", changed.profile().givenName()); assertEquals("D", changed.profile().nickname());
        assertEquals("new@example.com", changed.email()); assertFalse(changed.emailVerified());
        assertNull(ProfilePatch.builder().nickname(null).build().apply(changed, "actor", at).profile().nickname());
        assertThrows(DomainException.class, () -> ProfilePatch.builder().givenName(null).build().apply(user, "actor", at));
    }
    @Test void authenticationProjectionStillAppliesDirectDenial() {
        var user = user();
        var role = new Role(2L, UUID.randomUUID(), "ADMIN", null, true, true, SoftDeletion.active(), AuditMetadata.create("system", at));
        var permission = new Permission(3L, UUID.randomUUID(), "user:read", "user", "read", null, true, true, SoftDeletion.active(), AuditMetadata.create("system", at));
        when(users.findByUsernameOrEmail("dara")).thenReturn(Optional.of(user));
        when(assignments.findUserRoles(1)).thenReturn(List.of(new UserRole(11L, user.id(), role.id(), AssignmentMetadata.assign("actor", at, null, null))));
        when(assignments.findRolePermissions(Set.of(2L))).thenReturn(List.of(new RolePermission(12L, role.id(), permission.id(), AssignmentMetadata.assign("actor", at, null, null))));
        when(assignments.findUserPermissions(1)).thenReturn(List.of(new UserPermission(13L, user.id(), permission.id(), PermissionEffect.DENY, AssignmentMetadata.assign("actor", at, null, null))));
        when(roles.findByIds(Set.of(2L))).thenReturn(List.of(role)); when(permissions.findByIds(Set.of(3L))).thenReturn(List.of(permission));
        var result = new LoadUserForAuthenticationUseCase(context).execute("dara");
        assertEquals(Set.of("ROLE_ADMIN"), result.authorities()); assertEquals(user.uuid(), result.uuid());
        assertFalse(result.toString().contains("sensitive-hash"));
    }
    @Test void passwordCommandsNeverPrintPlaintextAndEnforceBcryptByteLimit() {
        String secret = "Str0ng!Pass2026";
        assertFalse(new RegisterUserCommand("d", "d@example.com", secret, secret, "D", "S", "en").toString().contains(secret));
        assertFalse(new CreateUserCommand("d", "d@example.com", secret, UserProfile.basic("D", "S", "en"), List.of()).toString().contains(secret));
        assertFalse(new ChangePasswordCommand(UUID.randomUUID(), secret, secret, secret).toString().contains(secret));
        assertFalse(new ResetUserPasswordCommand(UUID.randomUUID(), secret, secret).toString().contains(secret));
        assertThrows(ApplicationException.class, () -> Passwords.confirm("ក".repeat(25), "ក".repeat(25)));
        assertDoesNotThrow(() -> Passwords.confirm("ក".repeat(24), "ក".repeat(24)));
    }
    @Test void pagingRejectsUnboundedRequestsAndInjectedSortNames() {
        assertThrows(DomainException.class, () -> new UserFilterQuery(-1, 20, null, null, null, null));
        assertThrows(DomainException.class, () -> new RoleFilterQuery(0, 101, null, null, null));
        assertThrows(DomainException.class, () -> new PermissionFilterQuery(0, 20, "name;drop table users", null, null, null, null));
        assertEquals("createdAt,desc", new UserFilterQuery(0, 20, null, "  ", null, null).sort());
    }
    @Test void unknownLoginAttemptIsANoOpAndUsesWriteBoundary() {
        when(users.findByUsernameOrEmail("missing")).thenReturn(Optional.empty());
        new RecordLoginAttemptUseCase(context).execute("missing", false);
        verify(users, never()).save(any()); assertEquals(1, writes);
    }
    @Test void incompleteFederationDoesNotCreateInventedProfileData() {
        var command = new FederatedIdentityCommand(Provider.FACEBOOK, "subject", null, false, "Display Name", null, null,
                Map.of("access_token", "sensitive-provider-data"), null, null, null);
        when(identities.findByProviderAndSubject(Provider.FACEBOOK, "subject")).thenReturn(Optional.empty());
        var error = assertThrows(ApplicationException.class, () -> new FindOrProvisionFederatedUserUseCase(context).execute(command));
        assertEquals("profile_completion_required", error.code()); assertEquals(Set.of("email", "givenName", "familyName"), error.errors().keySet());
        verify(users, never()).save(any()); assertFalse(command.toString().contains("sensitive-provider-data"));
    }
}
