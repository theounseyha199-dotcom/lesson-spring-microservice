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

public final class ResetUserPasswordUseCase {
    private final UseCaseContext context;
    public ResetUserPasswordUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public void execute(ResetUserPasswordCommand command) {
        context.transactions().write(() -> {
            Checks.required(command, "command"); Passwords.confirm(command.newPassword(), command.confirmPassword());
            var user = context.lockedUser(command.userUuid());
            context.users().save(user.changePasswordHash(context.passwords().hash(command.newPassword()), context.actor(), context.now()));
        });
    }
}
