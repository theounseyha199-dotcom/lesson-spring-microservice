package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

public record UserRole(Long id, long userId, long roleId, AssignmentMetadata metadata) {
    public UserRole {
        Checks.id(id, "assignment id"); Checks.reference(userId, "userId"); Checks.reference(roleId, "roleId");
         Checks.required(metadata, "metadata");
    }
    public static UserRole assign(User user, Role role, String actor, Instant now,
            Instant expiresAt, String reason) {
        Checks.required(user, "user"); Checks.required(role, "role");
        Checks.require(user.active(), "user_unavailable", "User is inactive or deleted"); role.ensureAssignable();
        return new UserRole(null, Checks.reference(user.id(), "userId"), Checks.reference(role.id(), "roleId"),
                AssignmentMetadata.assign(actor, now, expiresAt, reason));
    }
    public UserRole revoke(String actor, Instant now, String reason) {
        return new UserRole(id, userId, roleId, metadata.revoke(actor, now, reason));
    }
}
