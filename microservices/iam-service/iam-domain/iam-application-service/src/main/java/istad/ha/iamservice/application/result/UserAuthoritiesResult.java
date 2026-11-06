package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record UserAuthoritiesResult(List<RoleAssignmentResult> roles, List<PermissionGrantResult> directPermissions, Set<String> effectivePermissions, Set<String> authorities) {
    public UserAuthoritiesResult { roles = List.copyOf(roles); directPermissions = List.copyOf(directPermissions); effectivePermissions = Set.copyOf(effectivePermissions); authorities = Set.copyOf(authorities); }
}
