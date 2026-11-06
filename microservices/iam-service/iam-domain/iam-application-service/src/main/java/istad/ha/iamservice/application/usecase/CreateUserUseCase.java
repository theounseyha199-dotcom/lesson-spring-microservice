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

public final class CreateUserUseCase {
    private final UseCaseContext context;
    public CreateUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public UserResult execute(CreateUserCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); Passwords.confirm(command.password(), command.password()); context.ids(command.roleUuids(), false);
            var candidate = User.create(command.username(), command.email(), context.passwords().hash(command.password()), command.profile(), context.actor(), context.now());
            context.uniqueUser(candidate.username(), candidate.email());
            var roles = command.roleUuids().isEmpty() ? List.of(context.defaultRole()) : command.roleUuids().stream().map(context::role).toList();
            roles.forEach(Role::ensureAssignable); var user = context.users().save(candidate);
            roles.forEach(role -> context.assignments().save(UserRole.assign(user, role, context.actor(), context.now(), null, "Administrator creation")));
            return context.userResult(user);
        });
    }
}
