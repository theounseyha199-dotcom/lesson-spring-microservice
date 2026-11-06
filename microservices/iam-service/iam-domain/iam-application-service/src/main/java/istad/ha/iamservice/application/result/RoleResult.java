package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record RoleResult(UUID uuid, String name, String description, boolean system, boolean active, List<PermissionAssignmentResult> permissions, Instant createdAt, Instant updatedAt, long version) {
    public RoleResult { permissions = List.copyOf(permissions); }
}
