package istad.ha.iamservice.domain.model;

import java.util.Set;

public record UserAuthorities(Set<String> roles, Set<String> effectivePermissions) {
    public UserAuthorities { roles = Set.copyOf(roles); effectivePermissions = Set.copyOf(effectivePermissions); }
    public Set<String> authorities() {
        var result = new java.util.TreeSet<>(effectivePermissions);
        roles.forEach(role -> result.add("ROLE_" + role));
        return java.util.Collections.unmodifiableSet(result);
    }
}
