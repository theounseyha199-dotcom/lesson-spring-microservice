package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

public record UserPermission(Long id, long userId, long permissionId, PermissionEffect effect, AssignmentMetadata metadata) {
    public UserPermission {
        Checks.id(id, "assignment id"); Checks.reference(userId, "userId"); Checks.reference(permissionId, "permissionId");
        Checks.required(effect, "effect"); Checks.required(metadata, "metadata");
    }
    public static UserPermission assign(User user, Permission permission, PermissionEffect effect, String actor, Instant now,
            Instant expiresAt, String reason) {
        Checks.required(user, "user"); Checks.required(permission, "permission");
        Checks.require(user.active(), "user_unavailable", "User is inactive or deleted"); permission.ensureAssignable();
        return new UserPermission(null, Checks.reference(user.id(), "userId"), Checks.reference(permission.id(), "permissionId"), effect,
                AssignmentMetadata.assign(actor, now, expiresAt, reason));
    }
    public UserPermission revoke(String actor, Instant now, String reason) {
        return new UserPermission(id, userId, permissionId, effect, metadata.revoke(actor, now, reason));
    }
}
