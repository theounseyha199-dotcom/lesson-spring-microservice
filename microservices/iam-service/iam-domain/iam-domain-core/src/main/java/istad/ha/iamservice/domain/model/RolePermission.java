package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

public record RolePermission(Long id, long roleId, long permissionId, AssignmentMetadata metadata) {
    public RolePermission {
        Checks.id(id, "assignment id"); Checks.reference(roleId, "roleId"); Checks.reference(permissionId, "permissionId");
         Checks.required(metadata, "metadata");
    }
    public static RolePermission assign(Role role, Permission permission, String actor, Instant now,
            Instant expiresAt, String reason) {
        Checks.required(role, "role"); Checks.required(permission, "permission");
        role.ensureAssignable(); permission.ensureAssignable();
        return new RolePermission(null, Checks.reference(role.id(), "roleId"), Checks.reference(permission.id(), "permissionId"),
                AssignmentMetadata.assign(actor, now, expiresAt, reason));
    }
    public RolePermission revoke(String actor, Instant now, String reason) {
        return new RolePermission(id, roleId, permissionId, metadata.revoke(actor, now, reason));
    }
}
