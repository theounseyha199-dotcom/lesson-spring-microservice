package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record GrantPermissionsToUserCommand(UUID userUuid, List<UUID> permissionUuids, PermissionEffect effect, Instant expiresAt, String reason) {
    public GrantPermissionsToUserCommand { permissionUuids = permissionUuids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(permissionUuids)); }
}
