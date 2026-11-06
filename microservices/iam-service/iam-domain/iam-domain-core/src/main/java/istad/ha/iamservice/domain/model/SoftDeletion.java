package istad.ha.iamservice.domain.model;

import java.time.Instant;
import istad.ha.iamservice.domain.rule.Checks;

public record SoftDeletion(boolean deleted, Instant deletedAt, String deletedBy) {
    public SoftDeletion {
        if (deleted) {
            Checks.required(deletedAt, "deletedAt"); deletedBy = Checks.text(deletedBy, "deletedBy", 100);
        } else {
            Checks.require(deletedAt == null && deletedBy == null, "validation", "Active records cannot have deletion metadata");
        }
    }
    public void validate(AuditMetadata audit) {
        if (deleted) {
            Checks.require(!deletedAt.isBefore(audit.createdAt()) && !deletedAt.isAfter(audit.updatedAt()),
                    "validation", "Deletion must fall within the audit timeline");
        }
    }
    public static SoftDeletion active() { return new SoftDeletion(false, null, null); }
    public static SoftDeletion deleted(String actor, Instant now) { return new SoftDeletion(true, now, actor); }
}
