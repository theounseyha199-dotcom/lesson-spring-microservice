package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record CreateUserCommand(String username, String email, String password, UserProfile profile, List<UUID> roleUuids) {
    public CreateUserCommand { roleUuids = roleUuids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(roleUuids)); }
    @Override public String toString() { return "CreateUserCommand[redacted]"; }
}
