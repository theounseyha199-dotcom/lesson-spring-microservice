package istad.ha.iamservice.application.usecase;

import istad.ha.iamservice.application.*;
import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import java.util.*;
import java.time.*;
import java.util.stream.Collectors;
import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.query.*;
import istad.ha.iamservice.application.support.*;

public final class UpdatePermissionUseCase {
    private final UseCaseContext context;
    public UpdatePermissionUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public PermissionResult execute(UpdatePermissionCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); var permission = context.permission(command.permissionUuid());
            var updated = permission.update(command.resource().orElse(permission.resource()), command.action().orElse(permission.action()),
                    command.description().orElse(permission.description()), context.actor(), context.now());
            return context.permissionResult(context.permissions().save(updated));
        });
    }
}
