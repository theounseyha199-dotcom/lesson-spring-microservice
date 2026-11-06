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

public final class GrantPermissionsToUserUseCase {
    private final UseCaseContext context;
    public GrantPermissionsToUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public List<PermissionGrantResult> execute(GrantPermissionsToUserCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); context.ids(command.permissionUuids(), true); Checks.required(command.effect(), "effect");
            var user = context.lockedUser(command.userUuid()); var existing = context.assignments().findUserPermissions(user.id());
            var result = new ArrayList<PermissionGrantResult>();
            for (UUID uuid : command.permissionUuids()) {
                var permission = context.permission(uuid); permission.ensureAssignable();
                if (existing.stream().anyMatch(a -> a.permissionId() == permission.id() && a.effect() == command.effect() && a.metadata().activeAt(context.now()))) throw new ApplicationException("already_assigned", "Permission effect is already assigned");
                var saved = context.assignments().save(UserPermission.assign(user, permission, command.effect(), context.actor(), context.now(), command.expiresAt(), command.reason()));
                result.add(context.permissionGrant(saved, permission));
            }
            return List.copyOf(result);
        });
    }
}
