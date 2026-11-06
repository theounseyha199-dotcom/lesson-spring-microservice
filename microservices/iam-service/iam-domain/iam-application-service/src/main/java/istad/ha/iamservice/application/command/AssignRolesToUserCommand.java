package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record AssignRolesToUserCommand(UUID userUuid, List<UUID> roleUuids, Instant expiresAt, String reason) {
    public AssignRolesToUserCommand { roleUuids = roleUuids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(roleUuids)); }
}
