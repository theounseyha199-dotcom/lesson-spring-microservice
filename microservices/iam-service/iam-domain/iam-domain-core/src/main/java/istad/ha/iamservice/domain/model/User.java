package istad.ha.iamservice.domain.model;

import java.time.Instant;
import java.util.UUID;
import istad.ha.iamservice.domain.rule.Checks;

public record User(Long id, UUID uuid, String username, String email, boolean emailVerified,
        String passwordHash, UserProfile profile, boolean enabled, boolean accountNonExpired,
        boolean accountNonLocked, boolean credentialsNonExpired, int failedLoginAttempts,
        Instant lastLoginAt, Instant passwordChangedAt, SoftDeletion deletion, AuditMetadata audit) {
    public User {
        Checks.id(id, "user id"); Checks.required(uuid, "uuid");
        username = Checks.text(username, "username", 50); email = Checks.email(email);
        passwordHash = Checks.optional(passwordHash, "passwordHash", 255);
        Checks.required(profile, "profile"); Checks.required(deletion, "deletion"); Checks.required(audit, "audit"); deletion.validate(audit);
        Checks.require(lastLoginAt == null || (!lastLoginAt.isBefore(audit.createdAt()) && !lastLoginAt.isAfter(audit.updatedAt())),
                "validation", "Login must fall within the audit timeline");
        Checks.require(passwordChangedAt == null || (!passwordChangedAt.isBefore(audit.createdAt()) && !passwordChangedAt.isAfter(audit.updatedAt())),
                "validation", "Password change must fall within the audit timeline");
        Checks.require(failedLoginAttempts >= 0, "validation", "Failed attempts cannot be negative");
    }
    public static User create(String username, String email, String passwordHash, UserProfile profile,
            String actor, Instant now) {
        return new User(null, UUID.randomUUID(), username, email, false, passwordHash, profile,
                true, true, true, true, 0, null, passwordHash == null ? null : now,
                SoftDeletion.active(), AuditMetadata.create(actor, now));
    }
    public boolean active() { return enabled && !deletion.deleted(); }
    public boolean canAuthenticate() { return active() && accountNonExpired && accountNonLocked && credentialsNonExpired; }
    public boolean hasPassword() { return passwordHash != null; }
    public String name() { return profile.name(); }
    private void ensurePresent() { Checks.require(!deletion.deleted(), "user_deleted", "User is deleted"); }
    public User updateProfile(UserProfile profile, String actor, Instant now) {
        ensurePresent();
        Checks.required(profile, "profile");
        Checks.require(java.util.Objects.equals(this.profile.phoneNumber(), profile.phoneNumber())
                || !profile.phoneNumberVerified(), "validation", "A changed phone must be verified again");
        return copy(profile, enabled, accountNonLocked, passwordHash, credentialsNonExpired,
                failedLoginAttempts, lastLoginAt, passwordChangedAt, deletion, audit.touch(actor, now));
    }
    public User changeEmail(String email, String actor, Instant now) {
        ensurePresent(); email = Checks.email(email);
        return new User(id, uuid, username, email, this.email.equals(email) && emailVerified, passwordHash, profile,
                enabled, accountNonExpired, accountNonLocked, credentialsNonExpired, failedLoginAttempts,
                lastLoginAt, passwordChangedAt, deletion, audit.touch(actor, now));
    }
    /** For trusted verification flows only, never a self-service profile flag. */
    public User verifyEmail(String actor, Instant now) {
        ensurePresent();
        return new User(id, uuid, username, email, true, passwordHash, profile, enabled, accountNonExpired,
                accountNonLocked, credentialsNonExpired, failedLoginAttempts, lastLoginAt, passwordChangedAt,
                deletion, audit.touch(actor, now));
    }
    public User setEnabled(boolean enabled, String actor, Instant now) {
        ensurePresent(); return copy(profile, enabled, accountNonLocked, passwordHash, credentialsNonExpired,
                failedLoginAttempts, lastLoginAt, passwordChangedAt, deletion, audit.touch(actor, now));
    }
    public User setLocked(boolean locked, String actor, Instant now) {
        ensurePresent(); return copy(profile, enabled, !locked, passwordHash, credentialsNonExpired,
                failedLoginAttempts, lastLoginAt, passwordChangedAt, deletion, audit.touch(actor, now));
    }
    public User changePasswordHash(String hash, String actor, Instant now) {
        ensurePresent(); hash = Checks.text(hash, "passwordHash", 255);
        return copy(profile, enabled, accountNonLocked, hash, true, 0, lastLoginAt, now,
                deletion, audit.touch(actor, now));
    }
    public User recordLogin(boolean success, String actor, Instant now) {
        ensurePresent();
        Checks.require(!success || canAuthenticate(), "account_unavailable", "Account cannot sign in");
        Checks.require(success || failedLoginAttempts < Integer.MAX_VALUE, "validation", "Failed attempts overflow");
        return copy(profile, enabled, accountNonLocked, passwordHash, credentialsNonExpired,
                success ? 0 : failedLoginAttempts + 1, success ? now : lastLoginAt,
                passwordChangedAt, deletion, audit.touch(actor, now));
    }
    public User delete(String actor, Instant now) {
        ensurePresent(); return copy(profile, false, accountNonLocked, passwordHash, credentialsNonExpired,
                failedLoginAttempts, lastLoginAt, passwordChangedAt, SoftDeletion.deleted(actor, now), audit.touch(actor, now));
    }
    private User copy(UserProfile profile, boolean enabled, boolean unlocked, String hash, boolean credentialsValid,
            int failures, Instant lastLogin, Instant passwordChanged, SoftDeletion deleted, AuditMetadata audit) {
        return new User(id, uuid, username, email, emailVerified, hash, profile, enabled, accountNonExpired,
                unlocked, credentialsValid, failures, lastLogin, passwordChanged, deleted, audit);
    }
}
