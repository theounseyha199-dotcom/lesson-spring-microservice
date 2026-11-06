package istad.ha.iamservice.infrastructure.config;

import istad.ha.iamservice.infrastructure.federation.FederationAuthorizationRequests;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;

@Configuration
public class FederationConfig {
    @Bean public FederationAuthorizationRequests federationAuthorizationRequests() { return new FederationAuthorizationRequests(); }
    @Bean public OAuth2AuthorizationRequestResolver federationAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(builder -> {
            String registration = builder.build().getAttribute("registration_id");
            if (java.util.Set.of("google", "github", "telegram").contains(registration))
                OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
        });
        return resolver;
    }
}
