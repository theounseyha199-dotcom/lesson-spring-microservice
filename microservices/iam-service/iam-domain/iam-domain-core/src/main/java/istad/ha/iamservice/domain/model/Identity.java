package istad.ha.iamservice.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import istad.ha.iamservice.domain.rule.Checks;
import istad.ha.iamservice.domain.rule.JsonAttributes;

public record Identity(Long id, UUID uuid, long userId, Provider provider, String providerSubject,
        String email, boolean emailVerified, String displayName, String username, String pictureUrl,
        Map<String, Object> attributes, Instant linkedAt, Instant lastLoginAt,
        SoftDeletion deletion, AuditMetadata audit) {
    public Identity {
        Checks.id(id, "identity id"); Checks.required(uuid, "uuid"); Checks.reference(userId, "userId");
        Checks.required(provider, "provider"); providerSubject = Checks.text(providerSubject, "providerSubject", 255);
        email = email == null ? null : Checks.email(email);
        Checks.require(email != null || !emailVerified, "validation", "Missing email cannot be verified");
        displayName = Checks.optional(displayName, "displayName", 150);
        username = Checks.optional(username, "provider username", 100); pictureUrl = Checks.url(pictureUrl, "pictureUrl");
        attributes = JsonAttributes.copy(attributes); Checks.required(linkedAt, "linkedAt");
        Checks.required(deletion, "deletion"); Checks.required(audit, "audit"); deletion.validate(audit);
        Checks.require(!linkedAt.isBefore(audit.createdAt()) && !linkedAt.isAfter(audit.updatedAt()),
                "validation", "Linking must fall within the audit timeline");
        Checks.require(lastLoginAt == null || !lastLoginAt.isAfter(audit.updatedAt()),
                "validation", "Login cannot follow audit update");
        Checks.require(lastLoginAt == null || !lastLoginAt.isBefore(linkedAt), "validation", "Login cannot precede linking");
    }
    public boolean active() { return !deletion.deleted(); }
    public Identity recordLogin(String actor, Instant now) {
        Checks.require(active(), "identity_deleted", "Identity is deleted");
        return new Identity(id, uuid, userId, provider, providerSubject, email, emailVerified, displayName,
                username, pictureUrl, attributes, linkedAt, now, deletion, audit.touch(actor, now));
    }
    public Identity unlink(User owner, java.util.Collection<Identity> identities, String actor, Instant now) {
        return istad.ha.iamservice.domain.rule.IdentityRules.unlink(owner, uuid, identities, actor, now);
    }
}
