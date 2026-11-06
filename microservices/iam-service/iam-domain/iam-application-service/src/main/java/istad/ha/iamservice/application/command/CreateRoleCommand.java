package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record CreateRoleCommand(String name, String description, List<UUID> permissionUuids) {
    public CreateRoleCommand { permissionUuids = permissionUuids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(permissionUuids)); }
}
