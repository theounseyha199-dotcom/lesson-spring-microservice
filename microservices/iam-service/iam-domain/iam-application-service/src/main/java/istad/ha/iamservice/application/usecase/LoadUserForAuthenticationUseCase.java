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

public final class LoadUserForAuthenticationUseCase {
    private final UseCaseContext context;
    public LoadUserForAuthenticationUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public AuthUserResult execute(String usernameOrEmail) {
        return context.transactions().read(() -> {
            String value = Checks.text(usernameOrEmail, "usernameOrEmail", 255);
            return context.auth(context.users().findByUsernameOrEmail(value).orElseThrow(() -> UseCaseContext.missing("User")));
        });
    }
}
