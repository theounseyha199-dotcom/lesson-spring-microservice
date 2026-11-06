package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record UpdateUserProfileCommand(UUID userUuid, ProfilePatch patch) {
}
