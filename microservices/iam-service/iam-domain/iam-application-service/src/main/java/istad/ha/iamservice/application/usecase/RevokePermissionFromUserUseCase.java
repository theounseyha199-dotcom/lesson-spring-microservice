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

public final class RevokePermissionFromUserUseCase {
    private final UseCaseContext context;
    public RevokePermissionFromUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public void execute(UUID userUuid, UUID permissionUuid, String reason) {
        context.transactions().write(() -> {
            Checks.text(reason, "reason", 255); var user = context.lockedUser(userUuid); var permission = context.permission(permissionUuid);
            var rows = context.assignments().findUserPermissions(user.id()).stream().filter(a -> a.permissionId() == permission.id() && a.metadata().revokedAt() == null).toList();
            if (rows.isEmpty()) throw UseCaseContext.missing("Permission grant");
            rows.forEach(row -> context.assignments().save(row.revoke(context.actor(), context.now(), reason)));
        });
    }
}
