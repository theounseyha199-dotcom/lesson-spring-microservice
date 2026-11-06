package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record ResetUserPasswordCommand(UUID userUuid, String newPassword, String confirmPassword) {
    @Override public String toString() { return "ResetUserPasswordCommand[redacted]"; }
}
