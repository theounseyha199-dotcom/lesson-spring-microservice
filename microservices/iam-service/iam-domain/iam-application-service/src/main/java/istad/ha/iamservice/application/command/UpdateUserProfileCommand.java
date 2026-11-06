package istad.ha.iamservice.application.command;
import java.util.UUID;
/** Optional expected version protects browser forms without changing existing API PATCH callers. */
public record UpdateUserProfileCommand(UUID userUuid, ProfilePatch patch, Long expectedVersion) {
    public UpdateUserProfileCommand(UUID userUuid, ProfilePatch patch) { this(userUuid, patch, null); }
}
