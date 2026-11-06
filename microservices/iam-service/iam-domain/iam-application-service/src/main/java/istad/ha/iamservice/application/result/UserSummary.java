package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record UserSummary(UUID uuid, String username, String email, String name, String locale, boolean enabled, Instant createdAt, Instant updatedAt) {}
