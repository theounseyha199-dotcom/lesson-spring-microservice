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

public final class RevokeRoleFromUserUseCase {
    private final UseCaseContext context;
    public RevokeRoleFromUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public void execute(UUID userUuid, UUID roleUuid, String reason) {
        context.transactions().write(() -> {
            Checks.text(reason, "reason", 255); var user = context.lockedUser(userUuid); var role = context.role(roleUuid);
            var rows = context.assignments().findUserRoles(user.id()).stream().filter(a -> a.roleId() == role.id() && a.metadata().revokedAt() == null).toList();
            if (rows.isEmpty()) throw UseCaseContext.missing("Role assignment");
            rows.forEach(row -> context.assignments().save(row.revoke(context.actor(), context.now(), reason)));
        });
    }
}
