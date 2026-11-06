package istad.ha.iamservice.infrastructure.init;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

@Component
public class InitRegisterClient implements ApplicationRunner {
    private final RegisteredClientRepository clients;
    private final PasswordEncoder encoder;
    private final String orderSecret;
    private final String oidcSecret;
    private final String registrationSecret;
    private final String postmanSecret, oidcClientId, redirectUri, postmanRedirectUri, logoutRedirectUri;

    public InitRegisterClient(RegisteredClientRepository clients, PasswordEncoder encoder,
            @Value("${ORDER_CLIENT_SECRET}") String orderSecret,
            @Value("${app.iam.client-secret}") String oidcSecret,
            @Value("${app.iam.registration-client-secret}") String registrationSecret,
            @Value("${app.iam.postman-client-secret}") String postmanSecret,
            @Value("${app.iam.client-id}") String oidcClientId,
            @Value("${app.iam.redirect-uri}") String redirectUri,
            @Value("${app.iam.postman-redirect-uri}") String postmanRedirectUri,
            @Value("${app.iam.logout-redirect-uri}") String logoutRedirectUri) {
        this.clients = clients;
        this.encoder = encoder;
        this.orderSecret = orderSecret;
        this.oidcSecret = oidcSecret;
        org.springframework.util.Assert.hasText(registrationSecret, "IAM_REGISTRATION_CLIENT_SECRET must not be blank");
        this.registrationSecret = registrationSecret;
        org.springframework.util.Assert.hasText(postmanSecret,"IAM_POSTMAN_CLIENT_SECRET is required");
        org.springframework.util.Assert.hasText(orderSecret,"ORDER_CLIENT_SECRET is required");
        org.springframework.util.Assert.hasText(oidcSecret,"IAM_CLIENT_SECRET is required");
        this.postmanSecret=postmanSecret; this.oidcClientId=oidcClientId; this.redirectUri=redirectUri;
        this.postmanRedirectUri=postmanRedirectUri; this.logoutRedirectUri=logoutRedirectUri;
    }

    @Override
    public void run(ApplicationArguments args) {
        save(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(oidcClientId).clientSecret(encoder.encode(oidcSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .redirectUri(postmanRedirectUri)
                .postLogoutRedirectUri(logoutRedirectUri)
                .scope("openid").scope("profile").scope("email").scope("phone").scope("address")
                .clientSettings(ClientSettings.builder().requireProofKey(true)
                        .requireAuthorizationConsent(true).build()).build());
        save(service("client-registrar", registrationSecret).scope("client.create").build());
        save(service("order-service", orderSecret).scope("customer.read").scope("customer.write").build());
        save(service("postman-client", postmanSecret).scope("api.read").scope("api.write").build());
    }

    private RegisteredClient.Builder service(String id, String secret) {
        return RegisteredClient.withId(UUID.randomUUID().toString()).clientId(id)
                .clientSecret(encoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    private void save(RegisteredClient client) {
        if (clients.findByClientId(client.getClientId()) == null) clients.save(client);
    }
}
