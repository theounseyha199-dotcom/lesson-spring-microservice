package istad.ha.iamservice.init;

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

    public InitRegisterClient(RegisteredClientRepository clients, PasswordEncoder encoder,
            @Value("${ORDER_CLIENT_SECRET}") String orderSecret,
            @Value("${app.iam.client-secret}") String oidcSecret) {
        this.clients = clients;
        this.encoder = encoder;
        this.orderSecret = orderSecret;
        this.oidcSecret = oidcSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        save(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oidc-client").clientSecret(encoder.encode(oidcSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .redirectUri("https://oauth.pstmn.io/v1/browser-callback")
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                .scope("openid").scope("profile")
                .clientSettings(ClientSettings.builder().requireProofKey(true)
                        .requireAuthorizationConsent(true).build()).build());
        save(service("order-service", orderSecret).scope("customer.read").scope("customer.write").build());
        save(service("postman-client", "dev-secret-change-me").scope("api.read").scope("api.write").build());
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
