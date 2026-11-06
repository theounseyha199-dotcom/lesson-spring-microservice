package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record CreatePermissionCommand(String resource, String action, String description) {
}
