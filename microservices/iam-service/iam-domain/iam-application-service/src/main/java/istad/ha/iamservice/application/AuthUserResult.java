package istad.ha.iamservice.application;

/** Authentication-only result; never serialize this object as an API response. */
public record AuthUserResult(java.util.UUID uuid, String username, String email, boolean emailVerified,
        istad.ha.iamservice.domain.model.UserProfile profile, String passwordHash, java.util.Set<String> authorities,
        boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled) {
    public AuthUserResult { authorities = java.util.Set.copyOf(authorities); }
    @Override public String toString() { return "AuthUserResult[uuid=" + uuid + ", username=" + username + "]"; }
}
