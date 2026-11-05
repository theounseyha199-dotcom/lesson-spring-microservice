package istad.ha.iamservice;

import istad.ha.iamservice.init.InitRegisterClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iam;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "spring.liquibase.change-log=classpath:db/test-changelog.yaml",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "ORDER_CLIENT_SECRET=test-order-secret", "spring.cloud.consul.enabled=false",
        "spring.security.oauth2.client.registration.github.client-id=test-github",
        "spring.security.oauth2.client.registration.github.client-secret=test-secret",
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "app.iam.client-secret=test-secret"})
@AutoConfigureMockMvc
class IamServiceApplicationTests {
    @TempDir static Path keys;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.iam.signing-key-file", () -> keys.resolve("signing.json").toString());
    }
    @Autowired RegisteredClientRepository clients;
    @Autowired InitRegisterClient initializer;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;

    @Test
    void migrationAndSeedingWorkAndRepeatedStartupPreservesClient() {
        var client = clients.findByClientId("oidc-client");
        assertNotNull(client);
        assertTrue(encoder.matches("test-secret", client.getClientSecret()));
        assertTrue(client.getClientSettings().isRequireProofKey());
        initializer.run(new DefaultApplicationArguments());
        var existing = clients.findByClientId("oidc-client");
        assertEquals(client.getId(), existing.getId());
        assertEquals(client.getClientSecret(), existing.getClientSecret());
    }

    @Test
    void serviceClientsAreSeededWithSeparateScopesAndPreservedSecrets() {
        var order = clients.findByClientId("order-service");
        assertNotNull(order);
        assertTrue(encoder.matches("test-order-secret", order.getClientSecret()));
        assertEquals(java.util.Set.of("customer.read", "customer.write"), order.getScopes());
        assertTrue(order.getAuthorizationGrantTypes().contains(
                org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS));
        var postman = clients.findByClientId("postman-client");
        assertEquals(java.util.Set.of("api.read", "api.write"), postman.getScopes());
        initializer.run(new DefaultApplicationArguments());
        assertEquals(order.getId(), clients.findByClientId("order-service").getId());
        assertEquals(order.getClientSecret(), clients.findByClientId("order-service").getClientSecret());
    }

    @Test
    void discoveryAndLoginAreAvailable() throws Exception {
        mvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:9000"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/oauth2/authorize").with(request -> {
                    request.setServerPort(9000);
                    return request;
                })
                .queryParam("response_type", "code").queryParam("client_id", "oidc-client")
                .queryParam("redirect_uri", "https://oauth.pstmn.io/v1/browser-callback")
                .queryParam("scope", "openid profile").queryParam("state", "test-state")
                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .queryParam("code_challenge_method", "S256")
                .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    @Test
    void customLoginRendersWithCsrfAndPublicAssets() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continue with GitHub")));
        mvc.perform(get("/css/login.css")).andExpect(status().isOk());
        mvc.perform(get("/js/login.js")).andExpect(status().isOk());
        mvc.perform(get("/login?error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to sign in.")));
        mvc.perform(get("/login?logout"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You have been signed out successfully.")));
    }

    @Test
    void loginAuthenticatesAndRejectsBadCredentialsAndMissingCsrf() throws Exception {
        var page = mvc.perform(get("/login")).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) page.getRequest().getSession();
        var csrf = (org.springframework.security.web.csrf.CsrfToken) page.getRequest().getAttribute("_csrf");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                        .session(session).param(csrf.getParameterName(), csrf.getToken())
                        .param("username", "user").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                        .session(session).param(csrf.getParameterName(), csrf.getToken())
                        .param("username", "user").param("password", "password"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                        .param("username", "user").param("password", "password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerLoginStartsAuthorizationAndFailureReturnsToForm() throws Exception {
        for (String provider : java.util.List.of("github", "google")) {
            mvc.perform(get("/oauth2/authorization/" + provider))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                            provider.equals("github") ? "github.com/login/oauth/authorize" : "accounts.google.com")));
        }
        mvc.perform(get("/login/oauth2/code/github").param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?oauth2error"));
        mvc.perform(get("/login?oauth2error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to sign in with your provider.")));
    }

}
