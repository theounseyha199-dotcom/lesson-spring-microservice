package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
import istad.ha.iamservice.domain.model.SoftDeletion;

@MappedSuperclass
@Getter @Setter
public abstract class SoftDeletableEntity extends AuditedEntity {
    @Column(nullable = false, unique = true, updatable = false) private UUID uuid;
    @Column(name = "is_deleted", nullable = false) private boolean deleted;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "deleted_by", length = 100) private String deletedBy;
    public void applyUuid(UUID value) {
        if (getId() != null && !uuid.equals(value)) {
            throw new istad.ha.iamservice.domain.exception.DomainException("immutable_identity", "Public UUID cannot be changed");
        }
        uuid = value;
    }
    public SoftDeletion deletion() { return new SoftDeletion(deleted, deletedAt, deletedBy); }
    public void applyDeletion(SoftDeletion deletion) {
        deleted = deletion.deleted(); deletedAt = deletion.deletedAt(); deletedBy = deletion.deletedBy();
    }
}
