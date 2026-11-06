package istad.ha.iamservice.domain.rule;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import istad.ha.iamservice.domain.model.Identity;
import istad.ha.iamservice.domain.model.User;

/** Invoke with the complete identity set in a transaction; persistence must enforce uniqueness too. */
public final class IdentityRules {
    private IdentityRules() { }
    public static void ensureCanLink(User user, Identity candidate, Collection<Identity> identities) {
        Checks.required(user, "user"); Checks.required(candidate, "identity"); Checks.required(identities, "identities");
        long userId = Checks.reference(user.id(), "userId");
        Checks.require(user.active(), "user_unavailable", "User is inactive or deleted");
        Checks.require(candidate.active() && candidate.userId() == userId, "identity_owner", "Identity owner does not match");
        for (Identity existing : identities) {
            Checks.require(existing.userId() != userId || existing.provider() != candidate.provider(),
                    "provider_already_linked", "User already has an identity for this provider");
            Checks.require(existing.provider() != candidate.provider()
                    || !existing.providerSubject().equals(candidate.providerSubject()),
                    "identity_already_linked", "Provider identity is already linked");
        }
    }
    public static Identity unlink(User user, UUID identityUuid, Collection<Identity> identities, String actor, Instant now) {
        Checks.required(user, "user"); Checks.required(identityUuid, "identityUuid"); Checks.required(identities, "identities");
        long userId = Checks.reference(user.id(), "userId");
        Checks.require(user.active(), "user_unavailable", "User is inactive or deleted");
        var target = identities.stream().filter(i -> i.userId() == userId && i.active() && i.uuid().equals(identityUuid))
                .findFirst().orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("identity_not_found", "Identity not found"));
        long alternatives = identities.stream().filter(i -> i.userId() == userId && i.active()
                && !i.uuid().equals(identityUuid)).count();
        Checks.require(user.hasPassword() || alternatives > 0, "last_sign_in_method", "Cannot unlink the last sign-in method");
        return new Identity(target.id(), target.uuid(), target.userId(), target.provider(), target.providerSubject(),
                target.email(), target.emailVerified(), target.displayName(), target.username(), target.pictureUrl(),
                target.attributes(), target.linkedAt(), target.lastLoginAt(),
                istad.ha.iamservice.domain.model.SoftDeletion.deleted(actor, now), target.audit().touch(actor, now));
    }
}
