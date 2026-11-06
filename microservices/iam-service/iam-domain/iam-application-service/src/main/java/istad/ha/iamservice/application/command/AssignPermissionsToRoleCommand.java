package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record AssignPermissionsToRoleCommand(UUID roleUuid, List<UUID> permissionUuids, Instant expiresAt, String reason) {
    public AssignPermissionsToRoleCommand { permissionUuids = permissionUuids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(permissionUuids)); }
}
