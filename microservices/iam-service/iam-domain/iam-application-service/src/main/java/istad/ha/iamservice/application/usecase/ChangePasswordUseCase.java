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

public final class ChangePasswordUseCase {
    private final UseCaseContext context;
    public ChangePasswordUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public void execute(ChangePasswordCommand command) {
        context.transactions().write(() -> {
            Checks.required(command, "command"); Passwords.confirm(command.newPassword(), command.confirmPassword());
            var user = context.lockedUser(command.userUuid());
            if (user.passwordHash() == null || !context.passwords().matches(command.currentPassword(), user.passwordHash())) {
                throw new ApplicationException("invalid_current_password", "Current password is incorrect");
            }
            context.users().save(user.changePasswordHash(context.passwords().hash(command.newPassword()), context.actor(), context.now()));
        });
    }
}
