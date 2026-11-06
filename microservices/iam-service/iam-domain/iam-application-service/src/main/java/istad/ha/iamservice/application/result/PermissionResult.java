package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record PermissionResult(UUID uuid, String name, String resource, String action, String description, boolean system, boolean active, Instant createdAt, Instant updatedAt, long version) {}
