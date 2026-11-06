package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DomainFixtures {
    static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");
    static final String ACTOR = "system";
    static final UserProfile PROFILE = UserProfile.basic("Dara", "Sok", "km");
    static User user(long id, String hash) {
        return new User(id, UUID.randomUUID(), "dara", "DARA@ISTAD.CO", false, hash, PROFILE,
                true, true, true, true, 0, null, null, SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW));
    }
    static Role role(long id, String name, boolean system) {
        return new Role(id, UUID.randomUUID(), name, null, system, true,
                SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW));
    }
    static Permission permission(long id, String action) {
        return new Permission(id, UUID.randomUUID(), "user:" + action, "user", action, null, false,
                true, SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW));
    }
    static Identity identity(long id, long userId, Provider provider, String subject) {
        return new Identity(id, UUID.randomUUID(), userId, provider, subject, null, false,
                null, null, null, Map.of(), NOW, null, SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW));
    }
    static Identity unlinked(Identity identity, String actor, Instant now) {
        return identity.unlink(user(identity.userId(), "hash"), List.of(identity), actor, now);
    }
    static void code(String expected, org.junit.jupiter.api.function.Executable operation) {
        assertEquals(expected, assertThrows(DomainException.class, operation).code());
    }
}
