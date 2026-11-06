package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class AuthorityResolverTest {
    @Test void directDenialWinsOverRoleAndDirectGrantsWithoutRemovingRoleAuthority() {
        var user = user(1, "hash"); var admin = role(2, "ADMIN", true);
        var read = permission(3, "read"); var write = permission(4, "update"); var delete = permission(5, "delete");
        var result = AuthorityResolver.resolve(user, List.of(admin), List.of(read, write, delete),
                List.of(UserRole.assign(user, admin, ACTOR, NOW, null, null)),
                List.of(RolePermission.assign(admin, read, ACTOR, NOW, null, null), RolePermission.assign(admin, write, ACTOR, NOW, null, null)),
                List.of(UserPermission.assign(user, delete, PermissionEffect.GRANT, ACTOR, NOW, null, null),
                        UserPermission.assign(user, write, PermissionEffect.GRANT, ACTOR, NOW, null, null),
                        UserPermission.assign(user, write, PermissionEffect.DENY, ACTOR, NOW, null, null)), NOW);
        assertEquals(Set.of("ADMIN"), result.roles());
        assertEquals(Set.of("user:read", "user:delete"), result.effectivePermissions());
        assertEquals(Set.of("ROLE_ADMIN", "user:read", "user:delete"), result.authorities());
        assertThrows(UnsupportedOperationException.class, () -> result.effectivePermissions().add("user:update"));
    }
    @Test void expiredDenialsStopApplyingAndOtherUsersAssignmentsAreIgnored() {
        var user = user(1, "hash"); var other = user(9, "hash"); var read = permission(3, "read"); var write = permission(4, "update");
        var grants = List.of(UserPermission.assign(user, read, PermissionEffect.GRANT, ACTOR, NOW, null, null),
                UserPermission.assign(user, read, PermissionEffect.DENY, ACTOR, NOW, NOW.plusSeconds(5), null),
                UserPermission.assign(other, write, PermissionEffect.GRANT, ACTOR, NOW, null, null));
        assertTrue(AuthorityResolver.resolve(user, List.of(), List.of(read, write), List.of(), List.of(), grants, NOW).authorities().isEmpty());
        assertEquals(Set.of("user:read"), AuthorityResolver.resolve(user, List.of(), List.of(read, write), List.of(), List.of(), grants, NOW.plusSeconds(5)).authorities());
    }
    @Test void revokedAndExpiredRolePathsDoNotContributePermissions() {
        var user = user(1, "hash"); var role = role(2, "EDITOR", false); var read = permission(3, "read");
        var membership = UserRole.assign(user, role, ACTOR, NOW, NOW.plusSeconds(10), null);
        var grant = RolePermission.assign(role, read, ACTOR, NOW, null, null);
        assertTrue(AuthorityResolver.resolve(user, List.of(role), List.of(read), List.of(membership), List.of(grant), List.of(), NOW.plusSeconds(10)).authorities().isEmpty());
        var revoked = membership.revoke(ACTOR, NOW.plusSeconds(2), "Removed");
        assertTrue(AuthorityResolver.resolve(user, List.of(role), List.of(read), List.of(revoked), List.of(grant), List.of(), NOW.plusSeconds(2)).authorities().isEmpty());
        var revokedGrant = grant.revoke(ACTOR, NOW.plusSeconds(2), "Removed");
        var result = AuthorityResolver.resolve(user, List.of(role), List.of(read), List.of(membership), List.of(revokedGrant), List.of(), NOW.plusSeconds(2));
        assertEquals(Set.of("ROLE_EDITOR"), result.authorities());
    }
    @Test void inactiveDeletedAndMissingRecordsCannotContributeAuthorities() {
        var user = user(1, "hash"); var role = role(2, "EDITOR", false); var read = permission(3, "read");
        var memberships = List.of(UserRole.assign(user, role, ACTOR, NOW, null, null));
        var grants = List.of(RolePermission.assign(role, read, ACTOR, NOW, null, null));
        var direct = List.of(UserPermission.assign(user, read, PermissionEffect.GRANT, ACTOR, NOW, null, null));
        assertTrue(AuthorityResolver.resolve(user, List.of(role.setActive(false, ACTOR, NOW)), List.of(read), memberships, grants, List.of(), NOW).authorities().isEmpty());
        assertTrue(AuthorityResolver.resolve(user, List.of(role.delete(ACTOR, NOW)), List.of(read), memberships, grants, List.of(), NOW).authorities().isEmpty());
        assertEquals(Set.of("ROLE_EDITOR"), AuthorityResolver.resolve(user, List.of(role), List.of(read.delete(ACTOR, NOW)), memberships, grants, direct, NOW).authorities());
        assertTrue(AuthorityResolver.resolve(user, List.of(), List.of(), memberships, grants, direct, NOW).authorities().isEmpty());
        assertTrue(AuthorityResolver.resolve(user.setEnabled(false, ACTOR, NOW), List.of(role), List.of(read), memberships, grants, direct, NOW).authorities().isEmpty());
    }
}
