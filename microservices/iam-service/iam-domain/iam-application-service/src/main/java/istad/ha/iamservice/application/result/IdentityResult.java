package istad.ha.iamservice.application.result;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record IdentityResult(UUID uuid, Provider provider, String email, boolean emailVerified, String displayName, String username, String pictureUrl, Instant linkedAt, Instant lastLoginAt) {}
