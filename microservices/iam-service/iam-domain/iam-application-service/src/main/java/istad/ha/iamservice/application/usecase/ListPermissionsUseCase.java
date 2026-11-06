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

public final class ListPermissionsUseCase {
    private final UseCaseContext context;
    public ListPermissionsUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public PageResult<PermissionResult> execute(PermissionFilterQuery query) {
        return context.transactions().read(() -> {
            Checks.required(query, "query"); return context.permissions().search(query).map(context::permissionResult);
        });
    }
}
