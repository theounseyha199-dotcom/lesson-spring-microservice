package istad.ha.iamservice.domain.model;

import java.time.Instant;
import java.util.UUID;
import istad.ha.iamservice.domain.rule.Checks;

public record Permission(Long id, UUID uuid, String name, String resource, String action, String description, boolean system,
        boolean active, SoftDeletion deletion, AuditMetadata audit) {
    public Permission {
        Checks.id(id, "id"); Checks.required(uuid, "uuid");
        resource = Checks.text(resource, "resource", 50); action = Checks.text(action, "action", 50);
        Checks.require(resource.matches("[a-z][a-z0-9_-]*") && action.matches("[a-z][a-z0-9_-]*"),
                "validation", "Permission resource and action must be lower-case identifiers");
        name = Checks.text(name, "name", 100);
        Checks.require(name.equals(resource + ":" + action), "validation", "Permission name must equal resource:action");
        description = Checks.optional(description, "description", 255);
        Checks.required(deletion, "deletion"); Checks.required(audit, "audit"); deletion.validate(audit);
    }
    public static Permission create(String resource, String action, String description, boolean system, String actor, Instant now) {
        return new Permission(null, UUID.randomUUID(), resource + ":" + action, resource, action, description, system, true,
                SoftDeletion.active(), AuditMetadata.create(actor, now));
    }
    public boolean assignable() { return active && !deletion.deleted(); }
    public void ensureAssignable() {
        Checks.require(assignable(), "inactive_assignment", "Cannot assign an inactive or deleted permission");
    }
    private void ensureMutable() {
        Checks.require(!system, "system_protected", "System permission cannot be changed or deleted");
        Checks.require(!deletion.deleted(), "record_deleted", "Record is deleted");
    }
    public Permission update(String resource, String action, String description, String actor, Instant now) {
        ensureMutable(); return copy(resource + ":" + action, resource, action, description, active, deletion, audit.touch(actor, now));
    }
    public Permission setActive(boolean active, String actor, Instant now) {
        ensureMutable(); return copy(name, resource, action, description, active, deletion, audit.touch(actor, now));
    }
    public Permission delete(String actor, Instant now) {
        ensureMutable(); return copy(name, resource, action, description, false, SoftDeletion.deleted(actor, now), audit.touch(actor, now));
    }
    private Permission copy(String name, String resource, String action, String description, boolean active, SoftDeletion deletion, AuditMetadata audit) {
        return new Permission(id, uuid, name, resource, action, description, system, active, deletion, audit);
    }
}
