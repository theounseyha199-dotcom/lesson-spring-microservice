package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class DomainValidationTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "not-an-email", "a@", "a b@example.com"})
    void rejectsInvalidRequiredEmail(String email) {
        code("validation", () -> User.create("dara", email, null, PROFILE, ACTOR, NOW));
    }
    @Test void enforcesColumnLengthsLocaleAndPermissionNameStructure() {
        code("validation", () -> User.create("x".repeat(51), "a@example.com", null, PROFILE, ACTOR, NOW));
        code("validation", () -> UserProfile.basic(" ", "Sok", "en"));
        code("validation", () -> UserProfile.basic("Dara", "Sok", "fr"));
        code("validation", () -> Role.create("R".repeat(51), null, false, ACTOR, NOW));
        code("validation", () -> Permission.create("user", "read:all", null, false, ACTOR, NOW));
        code("validation", () -> new Permission(1L, UUID.randomUUID(), "user:delete", "user", "read", null,
                false, true, SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW)));
    }
    @Test void validatesProfileFormatsAndRetainsEveryOidcProfileField() {
        var address = new Address("No. 24", "Toul Kork", "Phnom Penh", "120408", "kh");
        var profile = new UserProfile("Dara", "Sok", "Middle", "D", "MALE", java.time.LocalDate.of(2003, 5, 20),
                "https://example.com/profile", "https://example.com/photo", "https://example.com", "Asia/Phnom_Penh",
                "km", "+85512345678", true, address);
        assertEquals("KH", profile.address().country()); assertEquals("male", profile.gender());
        assertEquals("Middle", profile.middleName()); assertEquals("D", profile.nickname());
        assertEquals(java.time.LocalDate.of(2003, 5, 20), profile.birthdate());
        code("validation", () -> new Address(null, null, null, null, "XX"));
        code("validation", () -> new UserProfile("D", "S", null, null, null, null, null, null, null,
                "Unknown/Zone", "en", null, false, null));
        code("validation", () -> new UserProfile("D", "S", null, null, null, null, null, null, null,
                null, "en", "012345678", false, null));
        code("validation", () -> new UserProfile("D", "S", null, null, null, null, null, null, null,
                null, "en", null, true, null));
        code("validation", () -> Checks.url("javascript:alert(1)", "profile"));
    }
    @Test void auditAndDeletionMetadataRejectInconsistentHistory() {
        var audit = AuditMetadata.create("actor-sub", NOW);
        assertEquals("actor-sub", audit.createdBy()); assertEquals(0, audit.version());
        code("validation", () -> audit.touch(ACTOR, NOW.minusSeconds(1)));
        code("validation", () -> new AuditMetadata(NOW, ACTOR, NOW, ACTOR, -1));
        code("validation", () -> new SoftDeletion(false, NOW, ACTOR));
        code("validation", () -> new SoftDeletion(true, null, ACTOR));
        code("validation", () -> SoftDeletion.deleted(ACTOR, NOW.minusSeconds(1)).validate(audit));
    }
}
