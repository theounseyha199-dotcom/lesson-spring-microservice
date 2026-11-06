package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import istad.ha.iamservice.domain.model.AssignmentMetadata;

@MappedSuperclass @Getter @Setter
public abstract class AssignmentEntity extends AuditedEntity {
    @Column(name = "assigned_at", nullable = false, updatable = false) private Instant assignedAt;
    @Column(name = "assigned_by", nullable = false, length = 100, updatable = false) private String assignedBy;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revoked_by", length = 100) private String revokedBy;
    @Column(length = 255) private String reason;
    public AssignmentMetadata metadata() { return new AssignmentMetadata(assignedAt, assignedBy, expiresAt, revokedAt, revokedBy, reason, audit()); }
    public void applyMetadata(AssignmentMetadata value) {
        if (getId() != null && (!assignedAt.equals(value.assignedAt()) || !assignedBy.equals(value.assignedBy())
                || (revokedAt != null && (!revokedAt.equals(value.revokedAt()) || !revokedBy.equals(value.revokedBy())
                    || !java.util.Objects.equals(reason, value.reason()))))) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_history", "Assignment history cannot be rewritten");
        }
        applyAudit(value.audit()); assignedAt = value.assignedAt(); assignedBy = value.assignedBy();
        expiresAt = value.expiresAt(); revokedAt = value.revokedAt(); revokedBy = value.revokedBy(); reason = value.reason();
    }
}
