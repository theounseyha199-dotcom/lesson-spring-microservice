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

public final class UpdateUserProfileUseCase {
    private final UseCaseContext context;
    public UpdateUserProfileUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public UserResult execute(UpdateUserProfileCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); Checks.required(command.patch(), "patch");
            var user = context.user(command.userUuid());
            return context.userResult(context.users().save(command.patch().apply(user, context.actor(), context.now())));
        });
    }
}
