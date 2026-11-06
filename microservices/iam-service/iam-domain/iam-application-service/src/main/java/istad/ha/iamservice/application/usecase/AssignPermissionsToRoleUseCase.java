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

public final class AssignPermissionsToRoleUseCase {
    private final UseCaseContext context;
    public AssignPermissionsToRoleUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public List<PermissionAssignmentResult> execute(AssignPermissionsToRoleCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); context.ids(command.permissionUuids(), true);
            var role = context.lockedRole(command.roleUuid()); role.ensureAssignable();
            var existing = context.assignments().findRolePermissions(Set.of(role.id())); var result = new ArrayList<PermissionAssignmentResult>();
            for (UUID uuid : command.permissionUuids()) {
                var permission = context.permission(uuid); permission.ensureAssignable();
                if (existing.stream().anyMatch(a -> a.permissionId() == permission.id() && a.metadata().activeAt(context.now()))) throw new ApplicationException("already_assigned", "Permission is already assigned");
                var saved = context.assignments().save(RolePermission.assign(role, permission, context.actor(), context.now(), command.expiresAt(), command.reason()));
                result.add(context.permissionAssignment(saved, permission));
            }
            return List.copyOf(result);
        });
    }
}
