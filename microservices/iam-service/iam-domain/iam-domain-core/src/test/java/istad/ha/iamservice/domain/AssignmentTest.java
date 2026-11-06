package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class AssignmentTest {
    @Test void expiryIsExclusiveAndRevocationPreservesHistory() {
        var assigned = UserRole.assign(user(1, "hash"), role(2, "USER", true), "assigner", NOW,
                NOW.plusSeconds(60), "Temporary access");
        assertFalse(assigned.metadata().activeAt(NOW.minusNanos(1)));
        assertTrue(assigned.metadata().activeAt(NOW));
        assertTrue(assigned.metadata().activeAt(NOW.plusSeconds(60).minusNanos(1)));
        assertFalse(assigned.metadata().activeAt(NOW.plusSeconds(60)));
        var revoked = assigned.revoke("revoker", NOW.plusSeconds(10), "Access removed");
        assertTrue(revoked.metadata().activeAt(NOW.plusSeconds(9)));
        assertFalse(revoked.metadata().activeAt(NOW.plusSeconds(10)));
        assertEquals(NOW, revoked.metadata().assignedAt()); assertEquals("assigner", revoked.metadata().assignedBy());
        assertEquals(NOW.plusSeconds(60), revoked.metadata().expiresAt());
        assertEquals("revoker", revoked.metadata().revokedBy()); assertEquals("Access removed", revoked.metadata().reason());
        assertNull(assigned.metadata().revokedAt());
        code("assignment_revoked", () -> revoked.revoke(ACTOR, NOW.plusSeconds(11), "Again"));
    }
    @Test void allAssignmentTypesRejectInactiveTargets() {
        var disabledRole = role(2, "EDITOR", false).setActive(false, ACTOR, NOW);
        var disabledPermission = permission(3, "read").setActive(false, ACTOR, NOW);
        code("inactive_assignment", () -> UserRole.assign(user(1, "hash"), disabledRole, ACTOR, NOW, null, null));
        code("inactive_assignment", () -> RolePermission.assign(disabledRole, permission(3, "read"), ACTOR, NOW, null, null));
        code("inactive_assignment", () -> RolePermission.assign(role(2, "EDITOR", false), disabledPermission, ACTOR, NOW, null, null));
        code("inactive_assignment", () -> UserPermission.assign(user(1, "hash"), disabledPermission, PermissionEffect.GRANT, ACTOR, NOW, null, null));
        code("user_unavailable", () -> UserRole.assign(user(1, "hash").delete(ACTOR, NOW), role(2, "USER", true), ACTOR, NOW, null, null));
    }
    @Test void assignmentsValidatePersistedIdsExpiryAndRevocationMetadata() {
        var transientRole = Role.create("EDITOR", null, false, ACTOR, NOW);
        code("validation", () -> UserRole.assign(user(1, "hash"), transientRole, ACTOR, NOW, null, null));
        code("validation", () -> AssignmentMetadata.assign(ACTOR, NOW, NOW, null));
        code("validation", () -> AssignmentMetadata.assign(ACTOR, NOW, NOW.minusSeconds(1), null));
        var active = AssignmentMetadata.assign(ACTOR, NOW, null, null);
        code("validation", () -> active.revoke(ACTOR, NOW.minusSeconds(1), "Reason"));
        code("validation", () -> active.revoke(ACTOR, NOW, ""));
        code("validation", () -> new AssignmentMetadata(NOW, ACTOR, null, NOW, null, null, AuditMetadata.create(ACTOR, NOW)));
    }
    @Test void directAndRolePermissionAssignmentsCarryIndependentRevocationHistory() {
        var permission = permission(3, "read");
        var direct = UserPermission.assign(user(1, "hash"), permission, PermissionEffect.DENY, ACTOR, NOW, null, "Exception");
        var rolePermission = RolePermission.assign(role(2, "USER", true), permission, ACTOR, NOW, null, "Default");
        assertEquals(PermissionEffect.DENY, direct.revoke(ACTOR, NOW.plusSeconds(1), "Removed").effect());
        assertFalse(rolePermission.revoke(ACTOR, NOW.plusSeconds(1), "Removed").metadata().activeAt(NOW.plusSeconds(1)));
        assertTrue(direct.metadata().activeAt(NOW.plusSeconds(1)));
    }
}
