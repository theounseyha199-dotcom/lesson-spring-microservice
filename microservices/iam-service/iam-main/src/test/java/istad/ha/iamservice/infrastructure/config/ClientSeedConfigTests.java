package istad.ha.iamservice.infrastructure.config;
import istad.ha.iamservice.infrastructure.init.InitRegisterClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ClientSeedConfigTests {
    @Test void freshSeedsHonorExternallyConfiguredIdentityCallbacksAndSecrets() {
        var clients=mock(RegisteredClientRepository.class);var encoder=mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenAnswer(call -> "encoded:"+call.getArgument(0));
        new InitRegisterClient(clients,encoder,"test-order","test-oidc","test-registrar","test-postman","configured-client",
                "http://localhost/callback","http://localhost/postman","http://localhost/logout").run(null);
        var saved=ArgumentCaptor.forClass(RegisteredClient.class);verify(clients,times(4)).save(saved.capture());
        var oidc=saved.getAllValues().stream().filter(c -> c.getClientId().equals("configured-client")).findFirst().orElseThrow();
        assertEquals(java.util.Set.of("http://localhost/callback","http://localhost/postman"),oidc.getRedirectUris());
        assertEquals(java.util.Set.of("http://localhost/logout"),oidc.getPostLogoutRedirectUris());assertEquals("encoded:test-oidc",oidc.getClientSecret());
        assertEquals("encoded:test-postman",saved.getAllValues().stream().filter(c -> c.getClientId().equals("postman-client")).findFirst().orElseThrow().getClientSecret());
    }
}
