package istad.ha.iamservice.restapi.dto;

import istad.ha.iamservice.application.result.*;
import java.time.Instant;
import java.util.*;

/** Explicit public projections; authentication credentials and provider snapshots never leave the adapter. */
public final class Responses {
    private Responses() {}
    public record Role(UUID uuid, String name) {}
    public record User(UUID uuid, String username, String email, boolean emailVerified, String name,
            String locale, boolean enabled, List<Role> roles, Instant createdAt, Instant updatedAt) {}
    public static User user(UserResult result) {
        return new User(result.uuid(), result.username(), result.email(), result.emailVerified(), result.name(),
                result.profile().locale(), result.enabled(), result.roles().stream().map(r -> new Role(r.uuid(), r.name())).toList(), result.createdAt(), result.updatedAt());
    }
    public record AssignedRole(UUID uuid, String name, Instant expiresAt) {}
    public record DirectPermission(String name, String effect) {}
    public record Authorities(List<AssignedRole> roles, List<DirectPermission> directPermissions, Set<String> effectivePermissions) {}
    public static Authorities authorities(UserAuthoritiesResult result) {
        return new Authorities(result.roles().stream().map(r -> new AssignedRole(r.roleUuid(), r.name(), r.expiresAt())).toList(),
                result.directPermissions().stream().map(p -> new DirectPermission(p.name(), p.effect().name())).toList(), result.effectivePermissions());
    }
    public record Identity(UUID uuid, String provider, String email, String displayName, Instant linkedAt) {}
    public static Identity identity(IdentityResult result) {
        return new Identity(result.uuid(), result.provider().name(), result.email(), result.displayName(), result.linkedAt());
    }
}
