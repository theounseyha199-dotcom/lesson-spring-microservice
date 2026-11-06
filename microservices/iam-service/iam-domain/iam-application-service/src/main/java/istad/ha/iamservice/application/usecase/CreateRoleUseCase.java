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

public final class CreateRoleUseCase {
    private final UseCaseContext context;
    public CreateRoleUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public RoleResult execute(CreateRoleCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); context.ids(command.permissionUuids(), false);
            var candidate = Role.create(command.name(), command.description(), false, context.actor(), context.now());
            if (context.roles().findByName(candidate.name()).isPresent()) throw new ApplicationException("conflict", "Role name is already registered");
            var role = context.roles().save(candidate);
            for (UUID uuid : command.permissionUuids()) {
                var permission = context.permission(uuid);
                context.assignments().save(RolePermission.assign(role, permission, context.actor(), context.now(), null, "Role creation"));
            }
            return context.roleResult(role);
        });
    }
}
