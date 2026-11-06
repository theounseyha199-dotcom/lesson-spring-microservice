package istad.ha.iamservice;

import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.exception.DomainException;
import istad.ha.iamservice.bootstrap.InitCore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceIntegrationTests extends IamIntegrationTestSupport {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired IdentityRepository identities;
    @Autowired AssignmentRepository assignments;
    @Autowired UserAuthenticationPort authentication;
    @Autowired UserDetailsService userDetails;
    @Autowired JdbcTemplate jdbc;
    @Autowired InitCore initializer;
    @Autowired PlatformTransactionManager transactionManager;
    static Instant now() { return Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MICROS); }
    static String unique() { return UUID.randomUUID().toString().replace("-", ""); }
    User createUser(String password) {
        String suffix = unique();
        return users.save(User.create("u" + suffix, suffix + "@example.com", password,
                UserProfile.basic("Dara", "Sok", "km"), "test", now()));
    }
    Role createRole() { return roles.save(Role.create("R" + unique(), "Test role", false, "test", now())); }
    Permission createPermission() { return permissions.save(Permission.create("test_" + unique(), "read", "Test permission", false, "test", now())); }
    Identity identity(User user, Provider provider, String subject) {
        Instant at = now();
        return new Identity(null, UUID.randomUUID(), user.id(), provider, subject, null, false, "Dara", "dara",
                "https://example.com/avatar", Map.of("id", subject, "nested", Map.of("values", List.of("one", "two"))),
                at, null, SoftDeletion.active(), AuditMetadata.create("test", at));
    }
    @Test @Transactional
    void roundTripsEveryUserProfileFieldAndSoftDeletionAudit() {
        String suffix = unique(); Instant at = now();
        var profile = new UserProfile("Dara", "Sok", "Middle", "Nickname", "male", LocalDate.of(2003, 5, 20),
                "https://example.com/profile", "https://example.com/photo", "https://example.com", "Asia/Phnom_Penh", "km",
                "+855" + suffix.chars().filter(Character::isDigit).limit(8).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString(),
                true, new Address("No. 24", "Toul Kork", "Phnom Penh", "120408", "KH"));
        var saved = users.save(User.create("u" + suffix, suffix.toUpperCase() + "@EXAMPLE.COM", "unchanged-hash", profile, "actor-sub", at));
        assertEquals(saved, users.findByUuid(saved.uuid()).orElseThrow());
        assertEquals(saved, users.findByUsernameOrEmail(saved.email().toUpperCase()).orElseThrow());
        assertEquals(profile, saved.profile()); assertEquals("unchanged-hash", saved.passwordHash());
        var changed = users.save(saved.recordLogin(true, "actor-sub", at.plusSeconds(1)));
        assertEquals(saved.audit().version() + 1, changed.audit().version()); assertEquals(at.plusSeconds(1), changed.lastLoginAt());
        var deleted = users.save(changed.delete("admin-sub", at.plusSeconds(2)));
        assertTrue(deleted.deletion().deleted()); assertTrue(users.findByUuid(saved.uuid()).isEmpty());
        assertTrue(users.findByUsernameOrEmail(saved.username()).isEmpty()); assertTrue(users.usernameExists(saved.username()));
        assertEquals(1L, jdbc.queryForObject("select count(*) from users where id = ? and is_deleted = true and deleted_by = 'admin-sub'", Long.class, saved.id()));
    }
    @Test @Transactional
    void persistsAllAssignmentsAndAppliesDirectDenialInAuthentication() {
        var user = createUser("hash"); var role = createRole(); var permission = createPermission(); Instant at = now();
        var membership = assignments.save(UserRole.assign(user, role, "test", at, null, "Membership"));
        var grant = assignments.save(RolePermission.assign(role, permission, "test", at, null, "Role grant"));
        var denial = assignments.save(UserPermission.assign(user, permission, PermissionEffect.DENY, "test", at, null, "Exception"));
        assertTrue(authentication.findByUsername(user.username()).orElseThrow().authorities().contains("ROLE_" + role.name()));
        assertFalse(authentication.findByUsername(user.email()).orElseThrow().authorities().contains(permission.name()));
        var revoked = assignments.save(denial.revoke("admin-sub", at.plusMillis(1), "Exception ended"));
        assertEquals(denial.metadata().audit().version() + 1, revoked.metadata().audit().version());
        assertTrue(authentication.findByUsername(user.username()).orElseThrow().authorities().contains(permission.name()));
        var expired = assignments.save(UserPermission.assign(user, permission, PermissionEffect.DENY, "test", at, at.plusMillis(1), "Expired"));
        assertTrue(authentication.findByUsername(user.username()).orElseThrow().authorities().contains(permission.name()));
        assertEquals(2, assignments.findUserPermissions(user.id()).size());
        assertEquals(membership, assignments.findUserRoles(user.id()).get(0));
        assertEquals(grant, assignments.findRolePermissions(Set.of(role.id())).get(0));
        assertNotNull(expired.id());
    }
    @Test @Transactional
    void rejectsStaleUserSnapshotAndKeepsTheCommittedVersion() {
        var user = createUser("hash");
        var updated = users.save(user.setLocked(true, "owner", now().plusSeconds(2)));
        assertEquals(user.audit().version() + 1, updated.audit().version());
        assertThrows(OptimisticLockingFailureException.class, () -> users.save(user.setEnabled(false, "stale", now().plusSeconds(3))));
    }
    @Test @Transactional
    void versionsRolePermissionIdentityAndAssignmentUpdates() {
        var user = createUser("hash"); var role = createRole(); var permission = createPermission(); Instant at = now().plusSeconds(2);
        var changedRole = roles.save(role.update(role.name(), "New description", "test", at));
        assertEquals(role.audit().version() + 1, changedRole.audit().version());
        var changedPermission = permissions.save(permission.update(permission.resource(), "read", "New description", "test", at));
        assertEquals(permission.audit().version() + 1, changedPermission.audit().version());
        var identity = identities.save(identity(user, Provider.GITHUB, unique()));
        var changedIdentity = identities.save(identity.recordLogin("test", at));
        assertEquals(identity.audit().version() + 1, changedIdentity.audit().version());
        assertEquals(identity.attributes(), identities.findByUuid(identity.uuid()).orElseThrow().attributes());
        var membership = assignments.save(UserRole.assign(user, changedRole, "test", at, null, "Initial"));
        var revoked = assignments.save(membership.revoke("test", at.plusSeconds(1), "Removed"));
        assertEquals(membership.metadata().audit().version() + 1, revoked.metadata().audit().version());
        var grant = assignments.save(RolePermission.assign(changedRole, changedPermission, "test", at, null, "Initial"));
        assertEquals(grant.metadata().audit().version() + 1,
                assignments.save(grant.revoke("test", at.plusSeconds(1), "Removed")).metadata().audit().version());
    }
    @Test @Transactional
    void rejectsDuplicateNormalizedEmail() {
        var user = createUser("hash");
        assertThrows(DataIntegrityViolationException.class, () -> users.save(User.create("u" + unique(), user.email().toUpperCase(), null,
                UserProfile.basic("Dara", "Sok", "en"), "test", now())));
    }
    @Test @Transactional
    void rejectsDuplicateUsernamesAndReservesDeletedUserKeys() {
        var user = createUser("hash"); users.save(user.delete("test", now().plusSeconds(2)));
        assertThrows(DataIntegrityViolationException.class, () -> users.save(User.create(user.username(), unique() + "@example.com", null,
                UserProfile.basic("Dara", "Sok", "en"), "test", now())));
    }
    @Test @Transactional
    void rejectsDuplicateProviderSubjectAcrossUsers() {
        var first = createUser(null); var second = createUser(null); String subject = unique();
        identities.save(identity(first, Provider.GOOGLE, subject));
        assertThrows(DataIntegrityViolationException.class, () -> identities.save(identity(second, Provider.GOOGLE, subject)));
    }
    @Test @Transactional
    void rejectsSecondIdentityForSameProviderAndOwner() {
        var user = createUser("hash"); identities.save(identity(user, Provider.GOOGLE, unique()));
        assertThrows(DataIntegrityViolationException.class, () -> identities.save(identity(user, Provider.GOOGLE, unique())));
    }
    @Test @Transactional
    void unlinkKeepsIdentityHistoryAndUniqueKeys() {
        var user = createUser("hash"); var identity = identities.save(identity(user, Provider.FACEBOOK, unique()));
        var unlinked = identities.save(identity.unlink(user, identities.findByUserId(user.id()), "owner", now().plusSeconds(2)));
        assertTrue(unlinked.deletion().deleted()); assertEquals(1, identities.findByUserId(user.id()).size());
        assertTrue(identities.findByProviderAndSubject(identity.provider(), identity.providerSubject()).orElseThrow().deletion().deleted());
        assertThrows(DataIntegrityViolationException.class, () -> identities.save(identity(user, Provider.FACEBOOK, unique())));
    }
    @Test @Transactional
    void systemMetadataCannotBeBypassedWithAReconstructedDomainSnapshot() {
        var role = roles.findByName("ADMIN").orElseThrow();
        var changed = new Role(role.id(), role.uuid(), "OWNER", role.description(), false, true, role.deletion(), role.audit());
        assertEquals("system_protected", assertThrows(DomainException.class, () -> roles.save(changed)).code());
    }
    @Test @Transactional
    void repeatedSeedingPreservesPasswordsAndRevokedRoleGrants() {
        var user = users.findByUsernameOrEmail("user").orElseThrow();
        var admin = roles.findByName("ADMIN").orElseThrow();
        var grant = assignments.findRolePermissions(Set.of(admin.id())).get(0);
        assignments.save(grant.revoke("test", Instant.now(), "Explicit revocation"));
        initializer.run(new DefaultApplicationArguments());
        assertEquals(user.passwordHash(), users.findByUuid(user.uuid()).orElseThrow().passwordHash());
        assertEquals(1, assignments.findRolePermissions(Set.of(admin.id())).stream().filter(a -> a.permissionId() == grant.permissionId()).count());
        assertNotNull(assignments.findRolePermissions(Set.of(admin.id())).stream().filter(a -> a.id().equals(grant.id())).findFirst().orElseThrow().metadata().revokedAt());
    }
    @Test @Transactional
    void socialOnlyUserCannotUsePasswordAuthentication() {
        var user = createUser(null);
        assertFalse(userDetails.loadUserByUsername(user.username()).isEnabled());
    }
    @Test
    void parallelTransactionsCannotOverwriteTheSameLoadedUserVersion() throws Exception {
        var user = createUser("hash");
        var ready = new java.util.concurrent.CountDownLatch(2);
        var release = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var jobs = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int index = 0; index < 2; index++) {
                final int actor = index;
                jobs.add(pool.submit(() -> {
                    try {
                        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                            var loaded = users.findByUuid(user.uuid()).orElseThrow(); ready.countDown();
                            try { if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
                            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                            users.save(loaded.recordLogin(false, "actor-" + actor, Instant.now()));
                        });
                        return true;
                    } catch (OptimisticLockingFailureException ex) { return false; }
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)); release.countDown();
            int successes = 0;
            for (var job : jobs) if (job.get(15, java.util.concurrent.TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            assertEquals(1, users.findByUuid(user.uuid()).orElseThrow().failedLoginAttempts());
        } finally { release.countDown(); pool.shutdownNow(); }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"user_roles", "role_permissions", "user_permissions"})
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "IAM_TEST_DB_URL", matches = "jdbc:postgresql:.*")
    @Transactional
    void postgresBlocksPhysicalAssignmentDeletion(String table) {
        var user = createUser("hash"); var role = createRole(); var permission = createPermission();
        long id = switch (table) {
            case "user_roles" -> assignments.save(UserRole.assign(user, role, "test", now(), null, "History")).id();
            case "role_permissions" -> assignments.save(RolePermission.assign(role, permission, "test", now(), null, "History")).id();
            default -> assignments.save(UserPermission.assign(user, permission, PermissionEffect.GRANT, "test", now(), null, "History")).id();
        };
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("delete from " + table + " where id = ?", id));
    }
    @Test
    void ownerLockPreventsConcurrentUnlinkingOfEverySignInMethod() throws Exception {
        var user = createUser(null);
        var first = identities.save(identity(user, Provider.GOOGLE, unique()));
        var second = identities.save(identity(user, Provider.GITHUB, unique()));
        var ready = new java.util.concurrent.CountDownLatch(2);
        var release = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var jobs = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (var target : List.of(first, second)) {
                jobs.add(pool.submit(() -> {
                    ready.countDown();
                    if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                    try {
                        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                            var owner = users.findByUuidForUpdate(user.uuid()).orElseThrow();
                            var linked = identities.findByUserId(owner.id());
                            identities.save(istad.ha.iamservice.domain.rule.IdentityRules.unlink(owner, target.uuid(), linked, "owner", Instant.now()));
                        });
                        return true;
                    } catch (DomainException ex) {
                        assertEquals("last_sign_in_method", ex.code()); return false;
                    }
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)); release.countDown();
            int successes = 0;
            for (var job : jobs) if (job.get(15, java.util.concurrent.TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            assertEquals(1, identities.findByUserId(user.id()).stream().filter(Identity::active).count());
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
