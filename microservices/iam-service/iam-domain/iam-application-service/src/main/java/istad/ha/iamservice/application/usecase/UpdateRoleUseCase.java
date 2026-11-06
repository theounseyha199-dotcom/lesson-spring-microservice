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

public final class UpdateRoleUseCase {
    private final UseCaseContext context;
    public UpdateRoleUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public RoleResult execute(UpdateRoleCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); var role = context.role(command.roleUuid());
            var updated = role.update(command.name().orElse(role.name()), command.description().orElse(role.description()), context.actor(), context.now());
            return context.roleResult(context.roles().save(updated));
        });
    }
}
