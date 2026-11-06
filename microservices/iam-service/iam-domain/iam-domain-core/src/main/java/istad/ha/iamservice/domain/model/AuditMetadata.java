package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

/** Version is the last persisted optimistic-lock value; the persistence adapter advances it. */
public record AuditMetadata(Instant createdAt, String createdBy, Instant updatedAt, String updatedBy, long version) {
    public AuditMetadata {
        Checks.required(createdAt, "createdAt"); Checks.required(updatedAt, "updatedAt");
        createdBy = Checks.text(createdBy, "createdBy", 100);
        updatedBy = Checks.text(updatedBy, "updatedBy", 100);
        Checks.require(!updatedAt.isBefore(createdAt), "validation", "Audit time cannot move backwards");
        Checks.require(version >= 0, "validation", "Version cannot be negative");
    }
    public static AuditMetadata create(String actor, Instant now) {
        return new AuditMetadata(now, actor, now, actor, 0);
    }
    public AuditMetadata touch(String actor, Instant now) {
        Checks.required(now, "now");
        Checks.require(!now.isBefore(updatedAt), "validation", "Audit time cannot move backwards");
        return new AuditMetadata(createdAt, createdBy, now, actor, version);
    }
}
