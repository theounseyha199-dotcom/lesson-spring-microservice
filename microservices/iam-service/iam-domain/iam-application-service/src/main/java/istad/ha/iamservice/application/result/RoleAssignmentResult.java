package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record RoleAssignmentResult(long id, UUID roleUuid, String name, Instant assignedAt, String assignedBy, Instant expiresAt, Instant revokedAt, String revokedBy, String reason, long version) {}
