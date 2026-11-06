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

public final class LinkIdentityUseCase {
    private final UseCaseContext context;
    public LinkIdentityUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public IdentityResult execute(UUID userUuid, FederatedIdentityCommand command) {
        return context.transactions().write(() -> {
            Checks.required(command, "command"); var user = context.lockedUser(userUuid);
            Checks.require(user.active(), "user_unavailable", "User is inactive or deleted");
            var existing = context.identities().findByProviderAndSubject(command.provider(), command.providerSubject());
            if (existing.isPresent() && existing.get().userId() != user.id()) throw new ApplicationException("identity_already_linked", "Provider account is already linked");
            var candidate = context.providerSnapshot(user, existing.orElse(null), command, true);
            var relevant = context.identities().findByUserId(user.id()).stream().filter(i -> !i.uuid().equals(candidate.uuid())).toList();
            IdentityRules.ensureCanLink(user, candidate, relevant);
            return context.identityResult(context.identities().save(candidate));
        });
    }
}
