package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import istad.ha.iamservice.domain.model.AuditMetadata;

@MappedSuperclass
@Getter @Setter
public abstract class AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "created_by", nullable = false, length = 100, updatable = false) private String createdBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by", nullable = false, length = 100) private String updatedBy;
    public AuditMetadata audit() { return new AuditMetadata(createdAt, createdBy, updatedAt, updatedBy, version == null ? 0 : version); }
    public void applyAudit(AuditMetadata audit) {
        if (id != null && (version == null || version != audit.version())) {
            throw new OptimisticLockException("Stale IAM record");
        }
        if (id != null && (!java.util.Objects.equals(createdAt, audit.createdAt())
                || !java.util.Objects.equals(createdBy, audit.createdBy()))) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_history", "Creation audit cannot be changed");
        }
        createdAt = audit.createdAt(); createdBy = audit.createdBy();
        updatedAt = audit.updatedAt(); updatedBy = audit.updatedBy();
    }
}
