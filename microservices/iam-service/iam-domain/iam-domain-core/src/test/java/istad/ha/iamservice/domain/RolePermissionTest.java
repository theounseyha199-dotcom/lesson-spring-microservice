package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class RolePermissionTest {
    @Test void systemRecordsRejectAllMetadataLifecycleChanges() {
        var role = role(1, "ADMIN", true);
        code("system_protected", () -> role.update("OWNER", null, ACTOR, NOW));
        code("system_protected", () -> role.setActive(false, ACTOR, NOW));
        code("system_protected", () -> role.delete(ACTOR, NOW));
        var permission = Permission.create("user", "read", null, true, ACTOR, NOW);
        code("system_protected", () -> permission.update("user", "write", null, ACTOR, NOW));
        code("system_protected", () -> permission.setActive(false, ACTOR, NOW));
        code("system_protected", () -> permission.delete(ACTOR, NOW));
    }
    @Test void ordinaryRecordsCanChangeAndSoftDeleteWithAudit() {
        var role = role(1, "EDITOR", false).update("REVIEWER", "Review content", "admin", NOW.plusSeconds(1));
        assertEquals("REVIEWER", role.name()); assertEquals("admin", role.audit().updatedBy());
        assertFalse(role.setActive(false, ACTOR, NOW.plusSeconds(2)).assignable());
        var deleted = role.delete(ACTOR, NOW.plusSeconds(2));
        assertTrue(deleted.deletion().deleted()); assertFalse(deleted.assignable());
        code("record_deleted", () -> deleted.setActive(true, ACTOR, NOW.plusSeconds(3)));
        var permission = permission(1, "read").update("course", "publish", "Publish course", ACTOR, NOW);
        assertEquals("course:publish", permission.name());
        assertFalse(permission.delete(ACTOR, NOW).assignable());
    }
    @Test void seedsBothRolesAndEveryRequiredAdministrativePermission() {
        assertEquals(Set.of("ADMIN", "USER"), SystemDefaults.roles(ACTOR, NOW).stream().map(Role::name).collect(java.util.stream.Collectors.toSet()));
        assertTrue(SystemDefaults.roles(ACTOR, NOW).stream().allMatch(Role::system));
        var permissions = SystemDefaults.permissions(ACTOR, NOW);
        assertEquals(14, permissions.size()); assertTrue(permissions.stream().allMatch(Permission::system));
        assertTrue(SystemDefaults.permissionNamesFor("ADMIN").containsAll(List.of("user:assign", "role:assign", "permission:delete")));
        assertEquals(List.of("user:read", "role:read", "permission:read"), SystemDefaults.permissionNamesFor("USER"));
    }
}
