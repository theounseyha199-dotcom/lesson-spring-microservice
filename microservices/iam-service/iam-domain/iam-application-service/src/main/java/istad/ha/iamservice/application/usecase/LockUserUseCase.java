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

public final class LockUserUseCase {
    private final UseCaseContext context;
    public LockUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public void execute(UUID userUuid) {
        context.transactions().write(() -> {
            var user = context.lockedUser(userUuid);
            context.users().save(user.setLocked(true, context.actor(), context.now()));
        });
    }
}
