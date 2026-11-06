package istad.ha.iamservice.infrastructure.config;

import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.application.usecase.GetUserAuthoritiesUseCase;
import istad.ha.iamservice.application.usecase.GetUserUseCase;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.*;
import java.util.*;

@Configuration(proxyBeanMethods = false)
public class TokenConfig {
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(GetUserUseCase users,
            GetUserAuthoritiesUseCase authorities) {
        return context -> {
            boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
            boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
            if (!accessToken && !idToken) return;

            if (context.getPrincipal() != null && context.getPrincipal().getPrincipal() instanceof IamUserPrincipal principal) {
                UserResult user;
                try {
                    user = users.execute(principal.uuid());
                } catch (ApplicationException ex) {
                    if (!"not_found".equals(ex.code())) throw ex;
                    throw unavailable();
                }
                if (!user.enabled() || !user.accountNonExpired() || !user.accountNonLocked() || !user.credentialsNonExpired()) throw unavailable();
                context.getClaims().subject(user.uuid().toString());
                if (accessToken) {
                    // Re-evaluate grants on issuance/refresh, rather than trusting stale session authorities.
                    var effective = authorities.execute(user.uuid());
                    context.getClaims().claim("roles", effective.roles().stream().map(r -> r.name()).distinct().collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
                    context.getClaims().claim("permissions", new ArrayList<>(effective.effectivePermissions()));
                    context.getClaims().claim("authorities", new ArrayList<>(effective.authorities()));
                }
                if (idToken) addOidcClaims(context, user);
            }
            if (accessToken && context.getRegisteredClient() != null) {
                if ("order-service".equals(context.getRegisteredClient().getClientId())) {
                    context.getClaims().audience(new ArrayList<>(List.of("customer-service")));
                } else if (context.getAuthorizedScopes().contains("api.read") || context.getAuthorizedScopes().contains("api.write")) {
                    context.getClaims().audience(new ArrayList<>(List.of("order-service")));
                }
            }
        };
    }

    private static OAuth2AuthenticationException unavailable() {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "User account cannot sign in", null));
    }

    private static void addOidcClaims(JwtEncodingContext context, UserResult user) {
        var profile = user.profile();
        var scopes = context.getAuthorizedScopes();
        var claims = new LinkedHashMap<String, Object>();
        if (scopes.contains("profile")) {
            claims.put("name", user.name());
            claims.put("given_name", profile.givenName());
            claims.put("family_name", profile.familyName());
            claims.put("middle_name", profile.middleName());
            claims.put("nickname", profile.nickname());
            claims.put("preferred_username", user.username());
            claims.put("profile", profile.profileUrl());
            claims.put("picture", profile.pictureUrl());
            claims.put("website", profile.websiteUrl());
            claims.put("gender", profile.gender());
            claims.put("birthdate", profile.birthdate() == null ? null : profile.birthdate().toString());
            claims.put("zoneinfo", profile.zoneinfo());
            claims.put("locale", profile.locale());
            claims.put("updated_at", user.updatedAt().getEpochSecond());
        }
        if (scopes.contains("email")) {
            claims.put("email", user.email());
            claims.put("email_verified", user.emailVerified());
        }
        if (scopes.contains("phone") && profile.phoneNumber() != null) {
            claims.put("phone_number", profile.phoneNumber());
            claims.put("phone_number_verified", profile.phoneNumberVerified());
        }
        if (scopes.contains("address") && profile.address() != null) {
            var address = profile.address();
            var value = new LinkedHashMap<String, Object>();
            value.put("street_address", address.streetAddress());
            value.put("locality", address.locality());
            value.put("region", address.region());
            value.put("postal_code", address.postalCode());
            value.put("country", address.country());
            value.values().removeIf(Objects::isNull);
            claims.put("address", value);
        }
        claims.values().removeIf(Objects::isNull);
        context.getClaims().claims(values -> values.putAll(claims));
    }
}
