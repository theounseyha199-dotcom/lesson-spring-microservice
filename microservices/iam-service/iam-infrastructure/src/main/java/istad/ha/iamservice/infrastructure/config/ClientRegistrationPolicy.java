package istad.ha.iamservice.infrastructure.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.*;
import org.springframework.security.oauth2.server.authorization.oidc.converter.RegisteredClientOidcClientRegistrationConverter;
import java.util.*;

/** DCR creates consented OIDC clients, never privileged machine-to-machine clients. */
final class ClientRegistrationPolicy {
    private static final Set<String> SCOPES = Set.of("openid", "profile", "email", "phone", "address");
    private ClientRegistrationPolicy() {}

    static void configure(List<AuthenticationProvider> providers) {
        for (var provider : providers) {
            if (provider instanceof OidcClientRegistrationAuthenticationProvider registration) {
                registration.setAuthenticationValidator(
                        OidcClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                                .andThen(OidcClientRegistrationAuthenticationValidator.DEFAULT_POST_LOGOUT_REDIRECT_URI_VALIDATOR)
                                .andThen(OidcClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR)
                                .andThen(ClientRegistrationPolicy::validate));
            }
            if (provider instanceof OidcClientConfigurationAuthenticationProvider configuration) {
                var converter = new RegisteredClientOidcClientRegistrationConverter();
                configuration.setClientRegistrationConverter(client -> OidcClientRegistration
                        .withClaims(converter.convert(client).getClaims())
                        // The generated plaintext secret is returned once by POST; never expose its stored hash on GET.
                        .claims(claims -> claims.remove("client_secret")).build());
            }
        }
    }
    private static void validate(OidcClientRegistrationAuthenticationContext context) {
        OidcClientRegistration request = context.<OidcClientRegistrationAuthenticationToken>getAuthentication()codex.getClientRegistration();
        if (request == null) throw invalid("invalid_client_metadata");
        if (request.getScopes() == null || !request.getScopes().contains("openid") || !SCOPES.containsAll(request.getScopes())) {
            throw invalid(OAuth2ErrorCodes.INVALID_SCOPE);
        }
        if (request.getGrantTypes() != null && !Set.of("authorization_code", "refresh_token").containsAll(request.getGrantTypes())) {
            throw invalid("invalid_client_metadata");
        }
        if (request.getResponseTypes() != null && !Set.of("code").containsAll(request.getResponseTypes())) {
            throw invalid("invalid_client_metadata");
        }
        if (request.getTokenEndpointAuthenticationMethod() != null &&
                !"client_secret_basic".equals(request.getTokenEndpointAuthenticationMethod())) {
            throw invalid("invalid_client_metadata");
        }
    }
    private static OAuth2AuthenticationException invalid(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, "Unsupported OIDC client metadata", null));
    }
}
