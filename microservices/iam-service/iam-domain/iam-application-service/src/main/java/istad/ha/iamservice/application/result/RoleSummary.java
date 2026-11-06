package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record RoleSummary(UUID uuid, String name, boolean active) {}
