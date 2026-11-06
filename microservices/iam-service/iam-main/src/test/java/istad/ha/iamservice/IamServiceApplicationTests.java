package istad.ha.iamservice;

import istad.ha.iamservice.infrastructure.init.InitRegisterClient;
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

class IamServiceApplicationTests extends IamIntegrationTestSupport {
    @Autowired RegisteredClientRepository clients;
    @Autowired InitRegisterClient initializer;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @org.springframework.boot.test.web.server.LocalServerPort int port;

    @Test
    void loginRendersThroughEmbeddedServerWithoutForwardingToItself() throws Exception {
        var response = java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/login"))
                        .GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Continue with Telegram"));
        assertTrue(response.body().contains("Continue with Facebook"));
        assertTrue(response.body().contains("name=\"_csrf\""));
    }

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
    void unprovisionedProviderPrincipalsCannotOpenAnInternalProfile() throws Exception {
        mvc.perform(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("facebook-user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customLoginRendersWithCsrfAndPublicAssets() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continue with GitHub")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continue with Telegram")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/oauth2/authorization/telegram\"")));
        mvc.perform(get("/css/login.css")).andExpect(status().isOk());
        mvc.perform(get("/js/login.js")).andExpect(status().isOk());
        mvc.perform(get("/login?error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to sign in.")));
        mvc.perform(get("/login?logout"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You have been signed out successfully.")));
    }

    @Test
    void jteLoginUsesContextPathAndOnlyRendersActiveNotices() throws Exception {
        mvc.perform(get("/iam/login").contextPath("/iam"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/iam/login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/iam/css/account.css\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/iam/oauth2/authorization/telegram\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Unable to sign in."))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("You have been signed out successfully."))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("th:"))));
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
    void facebookLoginStartsAuthorizationAndHandlesCancellation() throws Exception {
        var response = mvc.perform(get("/oauth2/authorization/facebook"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        var location = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(response.getHeader("Location")).build();
        var params = location.getQueryParams();
        assertEquals("www.facebook.com", location.getHost());
        assertEquals("/dialog/oauth", location.getPath());
        assertEquals("test-facebook", params.getFirst("client_id"));
        assertEquals("code", params.getFirst("response_type"));
        assertEquals("https://lagging-grouped-bobsled.ngrok-free.dev/login/oauth2/code/facebook",
                params.getFirst("redirect_uri"));
        assertNotNull(params.getFirst("state"));
        assertTrue(params.getFirst("scope").contains("public_profile"));
        assertEquals("public_profile", params.getFirst("scope"));
        assertFalse(params.containsKey("client_secret"));
        mvc.perform(get("/login/oauth2/code/facebook").param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?oauth2error"));
    }

    @Test
    void telegramLoginStartsOidcAuthorization() throws Exception {
        mvc.perform(get("/oauth2/authorization/telegram"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://oauth.telegram.org/auth?")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("scope=openid%20profile")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=test-telegram")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code_challenge=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code_challenge_method=S256")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("redirect_uri=https://lagging-grouped-bobsled.ngrok-free.dev/login/oauth2/code/telegram")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")));
        mvc.perform(get("/login/oauth2/code/telegram").param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?oauth2error"));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Provider sign-in could not be completed.")));
    }

}
