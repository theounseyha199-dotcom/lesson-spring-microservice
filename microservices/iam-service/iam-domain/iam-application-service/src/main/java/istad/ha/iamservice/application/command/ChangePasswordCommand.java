package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record ChangePasswordCommand(UUID userUuid, String currentPassword, String newPassword, String confirmPassword) {
    @Override public String toString() { return "ChangePasswordCommand[redacted]"; }
}
