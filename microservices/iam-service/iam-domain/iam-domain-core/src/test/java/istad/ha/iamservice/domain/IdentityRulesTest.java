package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class IdentityRulesTest {
    @Test void rejectsDuplicateProviderPerUserAndProviderSubjectAcrossUsers() {
        var user = user(1, null); var google = identity(1, 1, Provider.GOOGLE, "g1");
        code("provider_already_linked", () -> IdentityRules.ensureCanLink(user,
                identity(2, 1, Provider.GOOGLE, "g2"), List.of(google)));
        code("identity_already_linked", () -> IdentityRules.ensureCanLink(user,
                google, List.of(identity(2, 9, Provider.GOOGLE, "g1"))));
        assertDoesNotThrow(() -> IdentityRules.ensureCanLink(user,
                identity(2, 1, Provider.FACEBOOK, "g1"), List.of(google)));
    }
    @Test void deletedIdentitiesStillReserveTheirUniqueProviderKeys() {
        var deleted = unlinked(identity(1, 9, Provider.GOOGLE, "g1"), ACTOR, NOW);
        code("identity_already_linked", () -> IdentityRules.ensureCanLink(user(1, null),
                identity(2, 1, Provider.GOOGLE, "g1"), List.of(deleted)));
    }
    @Test void cannotUnlinkFinalSignInMethodOrAnotherUsersIdentity() {
        var user = user(1, null); var google = identity(1, 1, Provider.GOOGLE, "g1");
        var other = identity(2, 9, Provider.FACEBOOK, "f1");
        code("last_sign_in_method", () -> google.unlink(user, List.of(google), ACTOR, NOW));
        code("last_sign_in_method", () -> IdentityRules.unlink(user, google.uuid(), List.of(google, other), ACTOR, NOW));
        code("identity_not_found", () -> IdentityRules.unlink(user, other.uuid(), List.of(google, other), ACTOR, NOW));
        var removed = unlinked(identity(3, 1, Provider.TELEGRAM, "t1"), ACTOR, NOW);
        code("last_sign_in_method", () -> IdentityRules.unlink(user, google.uuid(), List.of(google, removed), ACTOR, NOW));
    }
    @Test void passwordOrAnotherLinkedProviderAllowsAuditedSoftUnlink() {
        var google = identity(1, 1, Provider.GOOGLE, "g1"); var github = identity(2, 1, Provider.GITHUB, "gh1");
        var unlinked = IdentityRules.unlink(user(1, null), google.uuid(), List.of(google, github), "owner", NOW.plusSeconds(1));
        assertFalse(unlinked.active()); assertEquals("owner", unlinked.deletion().deletedBy());
        assertEquals(google.uuid(), unlinked.uuid()); assertEquals(google.providerSubject(), unlinked.providerSubject());
        assertTrue(google.active());
        assertFalse(IdentityRules.unlink(user(1, "hash"), google.uuid(), List.of(google), ACTOR, NOW).active());
    }
    @Test void checksOwnershipAndAvailabilityBeforeLinking() {
        var google = identity(1, 9, Provider.GOOGLE, "g1");
        code("identity_owner", () -> IdentityRules.ensureCanLink(user(1, "hash"), google, List.of()));
        code("user_unavailable", () -> IdentityRules.ensureCanLink(user(9, "hash").delete(ACTOR, NOW), google, List.of()));
    }
    @Test void recordsProviderLoginAndDefensivelyCopiesRawJsonAttributes() {
        var nested = new ArrayList<Object>(List.of("first"));
        var raw = new HashMap<String, Object>(); raw.put("nested", nested); raw.put("nullable", null);
        var original = new Identity(1L, UUID.randomUUID(), 1, Provider.GOOGLE, "g1", "DARA@ISTAD.CO", true,
                "Dara", "dara", "https://example.com/avatar", raw, NOW, null,
                SoftDeletion.active(), AuditMetadata.create(ACTOR, NOW));
        nested.add("second"); raw.clear();
        assertEquals(List.of("first"), original.attributes().get("nested")); assertTrue(original.attributes().containsKey("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> original.attributes().put("x", true));
        assertThrows(UnsupportedOperationException.class, () -> ((List<?>) original.attributes().get("nested")).clear());
        var signedIn = original.recordLogin(ACTOR, NOW.plusSeconds(1));
        assertEquals("dara@istad.co", signedIn.email()); assertEquals(NOW.plusSeconds(1), signedIn.lastLoginAt());
        code("identity_deleted", () -> unlinked(original, ACTOR, NOW).recordLogin(ACTOR, NOW));
        code("validation", () -> JsonAttributes.copy(Map.of("invalid", new Object())));
    }
}
