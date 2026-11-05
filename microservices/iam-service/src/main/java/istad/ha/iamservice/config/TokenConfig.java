package istad.ha.iamservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class TokenConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                if (context.getPrincipal() != null) {
                    Set<String> authorities = context.getPrincipal().getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    Set<String> roles = authorities.stream()
                            .filter(a -> a.startsWith("ROLE_"))
                            .map(a -> a.substring("ROLE_".length()))
                            .collect(Collectors.toSet());

                    if (!roles.isEmpty()) {
                        context.getClaims().claim("roles", roles);
                    }
                    if (!authorities.isEmpty()) {
                        context.getClaims().claim("authorities", authorities);
                    }
                }

                if (context.getRegisteredClient() != null) {
                    if ("order-service".equals(context.getRegisteredClient().getClientId())) {
                        context.getClaims().audience(List.of("customer-service"));
                    } else if (context.getAuthorizedScopes() != null &&
                            (context.getAuthorizedScopes().contains("api.read")
                                    || context.getAuthorizedScopes().contains("api.write"))) {
                        context.getClaims().audience(List.of("order-service"));
                    }
                }
            }
        };
    }
}
