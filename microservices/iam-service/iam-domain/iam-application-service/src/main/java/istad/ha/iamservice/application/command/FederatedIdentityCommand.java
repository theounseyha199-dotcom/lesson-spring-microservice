package istad.ha.iamservice.application.command;

import istad.ha.iamservice.domain.model.Provider;
import istad.ha.iamservice.domain.rule.*;

/** Construct only from a provider response whose OAuth/OIDC proof has been validated by an adapter. */
public record FederatedIdentityCommand(Provider provider, String providerSubject, String email, boolean emailVerified,
        String displayName, String username, String pictureUrl, java.util.Map<String, Object> attributes,
        String givenName, String familyName, String locale) {
    public FederatedIdentityCommand {
        Checks.required(provider, "provider"); providerSubject = Checks.text(providerSubject, "providerSubject", 255);
        email = email == null ? null : Checks.email(email);
        Checks.require(email != null || !emailVerified, "validation", "Missing email cannot be verified");
        displayName = Checks.optional(displayName, "displayName", 150);
        username = Checks.optional(username, "provider username", 100);
        pictureUrl = Checks.url(pictureUrl, "pictureUrl");
        givenName = Checks.optional(givenName, "givenName", 100); familyName = Checks.optional(familyName, "familyName", 100);
        locale = locale == null ? "en" : Checks.text(locale, "locale", 10);
        Checks.require(java.util.Set.of("en", "km").contains(locale), "validation", "Supported locales are en and km");
        attributes = JsonAttributes.copy(attributes);
    }
    @Override public String toString() { return "FederatedIdentityCommand[provider=" + provider + ", attributes=redacted]"; }
}
