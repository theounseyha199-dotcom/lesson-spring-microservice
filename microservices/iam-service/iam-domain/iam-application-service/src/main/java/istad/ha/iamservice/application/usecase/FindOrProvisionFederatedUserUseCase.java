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

public final class FindOrProvisionFederatedUserUseCase {
    private final UseCaseContext context;
    public FindOrProvisionFederatedUserUseCase(UseCaseContext context) { this.context = Objects.requireNonNull(context); }
    public AuthUserResult execute(FederatedIdentityCommand command) {
        Checks.required(command, "command");
        try { return context.transactions().write(() -> provision(command)); }
        catch (ApplicationException ex) {
            if (!"conflict".equals(ex.code())) throw ex;
            // Another first login may have won the unique provider key. Retry only that exact identity.
            return context.transactions().write(() -> {
                var existing = context.identities().findByProviderAndSubject(command.provider(), command.providerSubject());
                if (existing.isEmpty()) throw ex;
                return signIn(command, existing.get());
            });
        }
    }
    private AuthUserResult provision(FederatedIdentityCommand command) {
        var existing = context.identities().findByProviderAndSubject(command.provider(), command.providerSubject());
        if (existing.isPresent()) return signIn(command, existing.get());
        var missing = new LinkedHashMap<String, String>();
        if (command.email() == null) missing.put("email", "Complete your email address");
        if (command.givenName() == null) missing.put("givenName", "Complete your given name");
        if (command.familyName() == null) missing.put("familyName", "Complete your family name");
        if (!missing.isEmpty()) throw new ApplicationException("profile_completion_required", "Complete the required profile before creating an account", missing);
        if (context.users().emailExists(command.email())) throw new ApplicationException("identity_link_required", "Sign in to the existing account before linking this provider");
        String username = command.username();
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}") || context.users().usernameExists(username)) {
            username = command.provider().name().toLowerCase(Locale.ROOT) + "_" + UUID.randomUUID().toString().replace("-", "");
        }
        var profile = new UserProfile(command.givenName(), command.familyName(), null, null, null, null, null,
                command.pictureUrl(), null, null, command.locale(), null, false, null);
        var candidate = User.create(username, command.email(), null, profile, context.actor(), context.now());
        if (command.emailVerified()) candidate = candidate.verifyEmail(context.actor(), context.now());
        var role = context.defaultRole(); var user = context.users().save(candidate);
        context.assignments().save(UserRole.assign(user, role, context.actor(), context.now(), null, "Federated registration"));
        var identity = context.identities().save(context.providerSnapshot(user, null, command, false));
        return signIn(command, identity);
    }
    private AuthUserResult signIn(FederatedIdentityCommand command, Identity identity) {
        if (!identity.active()) throw new ApplicationException("identity_unlinked", "Sign in using another method to relink this provider");
        var owner = context.users().findById(identity.userId()).orElseThrow(() -> UseCaseContext.missing("User"));
        var user = context.lockedUser(owner.uuid());
        Checks.require(user.canAuthenticate(), "account_unavailable", "Account cannot sign in");
        // Re-read after the owner lock so an unlink cannot be undone by a simultaneous login.
        var current = context.identities().findByUuid(identity.uuid()).orElseThrow(() -> UseCaseContext.missing("Identity"));
        if (!current.active()) throw new ApplicationException("identity_unlinked", "Provider identity was unlinked");
        context.identities().save(context.providerSnapshot(user, current, command, false).recordLogin(context.actor(), context.now()));
        user = context.users().save(user.recordLogin(true, user.uuid().toString(), context.now()));
        return context.auth(user);
    }
}
