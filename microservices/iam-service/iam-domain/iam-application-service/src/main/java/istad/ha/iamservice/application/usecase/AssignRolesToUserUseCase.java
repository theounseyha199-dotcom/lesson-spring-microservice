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

public final class AssignRolesToUserUseCase {
    private final UseCaseContext context;
    public AssignRolesToUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public List<RoleAssignmentResult> execute(AssignRolesToUserCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); context.ids(command.roleUuids(), true);
            var user = context.lockedUser(command.userUuid()); var existing = context.assignments().findUserRoles(user.id());
            var result = new ArrayList<RoleAssignmentResult>();
            for (UUID uuid : command.roleUuids()) {
                var role = context.role(uuid); role.ensureAssignable();
                if (existing.stream().anyMatch(a -> a.roleId() == role.id() && a.metadata().activeAt(context.now()))) throw new ApplicationException("already_assigned", "Role is already assigned");
                var saved = context.assignments().save(UserRole.assign(user, role, context.actor(), context.now(), command.expiresAt(), command.reason()));
                result.add(context.roleAssignment(saved, role));
            }
            return List.copyOf(result);
        });
    }
}
