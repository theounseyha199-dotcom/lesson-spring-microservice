package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record UserResult(UUID uuid, String username, String email, boolean emailVerified, String name, UserProfile profile, boolean enabled, boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, int failedLoginAttempts, Instant lastLoginAt, Instant passwordChangedAt, List<RoleSummary> roles, Instant createdAt, Instant updatedAt, long version) {
    public UserResult { roles = List.copyOf(roles); }
}
