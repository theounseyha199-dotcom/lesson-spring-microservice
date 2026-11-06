package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

/** Assignment history is revoked, never deleted. Audit includes a persistence-managed version. */
public record AssignmentMetadata(Instant assignedAt, String assignedBy, Instant expiresAt,
        Instant revokedAt, String revokedBy, String reason, AuditMetadata audit) {
    public AssignmentMetadata {
        Checks.required(assignedAt, "assignedAt"); assignedBy = Checks.text(assignedBy, "assignedBy", 100);
        reason = Checks.optional(reason, "reason", 255); Checks.required(audit, "audit");
        Checks.require(!assignedAt.isBefore(audit.createdAt()) && !assignedAt.isAfter(audit.updatedAt()),
                "validation", "Assignment must fall within the audit timeline");
        Checks.require(expiresAt == null || expiresAt.isAfter(assignedAt), "validation", "Expiry must follow assignment");
        Checks.require((revokedAt == null) == (revokedBy == null), "validation", "Revocation requires time and actor");
        if (revokedAt != null) {
            revokedBy = Checks.text(revokedBy, "revokedBy", 100);
            Checks.require(!revokedAt.isBefore(assignedAt) && !revokedAt.isAfter(audit.updatedAt()), "validation", "Revocation cannot precede assignment");
        }
    }
    public static AssignmentMetadata assign(String actor, Instant now, Instant expiresAt, String reason) {
        return new AssignmentMetadata(now, actor, expiresAt, null, null, reason, AuditMetadata.create(actor, now));
    }
    public boolean activeAt(Instant now) {
        Checks.required(now, "now");
        return !now.isBefore(assignedAt) && (revokedAt == null || now.isBefore(revokedAt))
                && (expiresAt == null || now.isBefore(expiresAt));
    }
    public AssignmentMetadata revoke(String actor, Instant now, String reason) {
        Checks.require(revokedAt == null, "assignment_revoked", "Assignment already revoked");
        return new AssignmentMetadata(assignedAt, assignedBy, expiresAt, now, actor,
                Checks.text(reason, "revocation reason", 255), audit.touch(actor, now));
    }
}
