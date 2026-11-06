package istad.ha.iamservice.domain;

import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import istad.ha.iamservice.domain.exception.DomainException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static istad.ha.iamservice.domain.DomainFixtures.*;

class UserTest {
    @Test void createsUuidNormalizesEmailAndKeepsSocialOnlyPasswordNullable() {
        var user = User.create("dara", " DARA@ISTAD.CO ", null, PROFILE, ACTOR, NOW);
        assertNotNull(user.uuid()); assertNull(user.id()); assertEquals("dara@istad.co", user.email());
        assertEquals("Dara Sok", user.name()); assertFalse(user.hasPassword()); assertTrue(user.canAuthenticate());
        assertFalse(user.emailVerified()); assertNull(user.passwordChangedAt());
    }
    @Test void recordsConsecutiveFailuresAndResetsThemOnSuccess() {
        var initial = user(1, "hash");
        var failed = initial.recordLogin(false, ACTOR, NOW.plusSeconds(1)).recordLogin(false, ACTOR, NOW.plusSeconds(2));
        assertEquals(0, initial.failedLoginAttempts()); assertEquals(2, failed.failedLoginAttempts());
        assertNull(failed.lastLoginAt());
        var success = failed.recordLogin(true, ACTOR, NOW.plusSeconds(3));
        assertEquals(0, success.failedLoginAttempts()); assertEquals(NOW.plusSeconds(3), success.lastLoginAt());
        assertEquals(NOW, success.audit().createdAt()); assertEquals(NOW.plusSeconds(3), success.audit().updatedAt());
    }
    @Test void disabledLockedExpiredAndDeletedAccountsCannotAuthenticate() {
        var initial = user(1, "hash");
        assertFalse(initial.setEnabled(false, ACTOR, NOW).canAuthenticate());
        var locked = initial.setLocked(true, ACTOR, NOW);
        assertFalse(locked.canAuthenticate()); assertTrue(locked.setLocked(false, ACTOR, NOW).canAuthenticate());
        code("account_unavailable", () -> locked.recordLogin(true, ACTOR, NOW));
        var expired = new User(1L, initial.uuid(), "dara", initial.email(), false, "hash", PROFILE,
                true, false, true, true, 0, null, null, initial.deletion(), initial.audit());
        assertFalse(expired.canAuthenticate());
        var stalePassword = new User(1L, initial.uuid(), "dara", initial.email(), false, "hash", PROFILE,
                true, true, true, false, 0, null, null, initial.deletion(), initial.audit());
        assertFalse(stalePassword.canAuthenticate());
        var deleted = initial.delete("admin-sub", NOW.plusSeconds(1));
        assertFalse(deleted.canAuthenticate()); assertTrue(deleted.deletion().deleted());
        assertEquals("admin-sub", deleted.deletion().deletedBy());
        code("user_deleted", () -> deleted.setEnabled(true, ACTOR, NOW.plusSeconds(2)));
    }
    @Test void changesPasswordHashAndPreservesPersistenceVersion() {
        var updated = user(1, null).recordLogin(false, ACTOR, NOW).changePasswordHash("new-hash", ACTOR, NOW.plusSeconds(1));
        assertTrue(updated.hasPassword()); assertEquals("new-hash", updated.passwordHash());
        assertEquals(0, updated.failedLoginAttempts()); assertEquals(NOW.plusSeconds(1), updated.passwordChangedAt());
        assertEquals(0, updated.audit().version());
        code("validation", () -> updated.changePasswordHash("", ACTOR, NOW.plusSeconds(2)));
    }
    @Test void profileChangeRequiresFreshPhoneVerification() {
        var changed = new UserProfile("Dara", "Sok", null, "D", "male", java.time.LocalDate.of(2003, 5, 20),
                null, null, null, "Asia/Phnom_Penh", "en", "+85512345678", true, null);
        code("validation", () -> user(1, "hash").updateProfile(changed, ACTOR, NOW));
        var unverified = new UserProfile("Dara", "Sok", null, "D", "male", changed.birthdate(),
                null, null, null, changed.zoneinfo(), "en", changed.phoneNumber(), false, null);
        assertEquals("en", user(1, "hash").updateProfile(unverified, ACTOR, NOW).profile().locale());
    }
}
