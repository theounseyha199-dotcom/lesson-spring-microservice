package istad.ha.iamservice.domain.model;

import java.time.Instant;
import java.util.UUID;
import istad.ha.iamservice.domain.rule.Checks;

public record Role(Long id, UUID uuid, String name, String description, boolean system,
        boolean active, SoftDeletion deletion, AuditMetadata audit) {
    public Role {
        Checks.id(id, "id"); Checks.required(uuid, "uuid");
        name = Checks.text(name, "name", 50);
        description = Checks.optional(description, "description", 255);
        Checks.required(deletion, "deletion"); Checks.required(audit, "audit"); deletion.validate(audit);
    }
    public static Role create(String name, String description, boolean system, String actor, Instant now) {
        return new Role(null, UUID.randomUUID(), name, description, system, true,
                SoftDeletion.active(), AuditMetadata.create(actor, now));
    }
    public boolean assignable() { return active && !deletion.deleted(); }
    public void ensureAssignable() {
        Checks.require(assignable(), "inactive_assignment", "Cannot assign an inactive or deleted role");
    }
    private void ensureMutable() {
        Checks.require(!system, "system_protected", "System role cannot be changed or deleted");
        Checks.require(!deletion.deleted(), "record_deleted", "Record is deleted");
    }
    public Role update(String name, String description, String actor, Instant now) {
        ensureMutable(); return copy(name, description, active, deletion, audit.touch(actor, now));
    }
    public Role setActive(boolean active, String actor, Instant now) {
        ensureMutable(); return copy(name, description, active, deletion, audit.touch(actor, now));
    }
    public Role delete(String actor, Instant now) {
        ensureMutable(); return copy(name, description, false, SoftDeletion.deleted(actor, now), audit.touch(actor, now));
    }
    private Role copy(String name, String description, boolean active, SoftDeletion deletion, AuditMetadata audit) {
        return new Role(id, uuid, name, description, system, active, deletion, audit);
    }
}
