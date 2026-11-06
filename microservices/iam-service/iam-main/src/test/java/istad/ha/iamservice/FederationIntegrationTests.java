package istad.ha.iamservice;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.infrastructure.federation.*;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Real Spring OAuth callback filters, token exchanges, UserInfo and signed ID tokens against a local provider. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FederationIntegrationTests extends IamIntegrationTestSupport {
    static final String PASSWORD = "Str0ng!Pass2026";
    static final JsonMapper JSON = new JsonMapper();
    static final RSAKey KEY;
    static final HttpServer SERVER;
    static volatile Map<String, Object> claims;
    static volatile String nonce;
    static volatile String failure;
    static volatile boolean receivedVerifier;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("provider-key").generate();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/", exchange -> {
                try {
                    String path = exchange.getRequestURI().getPath();
                    Object body;
                    int status = 200;
                    if (path.equals("/jwks")) body = new JWKSet(KEY.toPublicJWK()).toJSONObject();
                    else if (path.startsWith("/token/")) {
                        String provider = path.substring("/token/".length());
                        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        receivedVerifier = requestBody.contains("code_verifier=");
                        if ("token".equals(failure)) { body = Map.of("error", "invalid_grant"); status = 400; }
                        else {
                            var token = new LinkedHashMap<String, Object>();
                            token.put("access_token", "test-provider-access-token"); token.put("token_type", "Bearer"); token.put("expires_in", 300);
                            token.put("scope", provider.equals("google") ? "openid profile email" : provider.equals("telegram") ? "openid profile" : "read:user user:email public_profile");
                            if (Set.of("google", "telegram").contains(provider)) token.put("id_token", signedToken(provider));
                            body = token;
                        }
                    } else body = claims;
                    byte[] bytes = JSON.writeValueAsBytes(body);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
                } catch (Exception ex) { exchange.sendResponseHeaders(500, -1); }
                finally { exchange.close(); }
            });
            SERVER.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    static String signedToken(String provider) throws Exception {
        var builder = new JWTClaimsSet.Builder().issuer("issuer".equals(failure) ? "https://attacker.invalid" :
                provider.equals("google") ? "https://accounts.google.com" : "https://oauth.telegram.org")
                .subject(claims.get("sub").toString()).audience("audience".equals(failure) ? "another-client" : "test-" + provider)
                .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("nonce", "nonce".equals(failure) ? "wrong-nonce" : nonce);
        claims.forEach((key, value) -> { if (!key.equals("sub")) builder.claim(key, value); });
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), builder.build());
        jwt.sign(new RSASSASigner("signature".equals(failure) ? new RSAKeyGenerator(2048).generate() : KEY));
        return jwt.serialize();
    }
    @DynamicPropertySource static void providers(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + SERVER.getAddress().getPort();
        for (String provider : List.of("google", "github", "facebook", "telegram")) {
            registry.add("spring.security.oauth2.client.provider." + provider + ".token-uri", () -> base + "/token/" + provider);
            registry.add("spring.security.oauth2.client.provider." + provider + ".user-info-uri", () -> base + "/userinfo/" + provider);
            if (Set.of("google", "telegram").contains(provider)) {
                registry.add("spring.security.oauth2.client.provider." + provider + ".jwk-set-uri", () -> base + "/jwks");
            }
            registry.add("spring.security.oauth2.client.registration." + provider + ".redirect-uri", () -> "http://localhost/login/oauth2/code/" + provider);
        }
    }
    void preview(String name, String html) throws Exception {
        String directory = System.getenv("IAM_UI_PREVIEW_DIR");
        if (directory != null) {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(directory));
            java.nio.file.Files.writeString(java.nio.file.Path.of(directory, name), html);
        }
    }
    @AfterAll static void stop() { SERVER.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext beans;
    <T> T use(Class<T> type) { return beans.getBean(type); }
    @BeforeEach void profile() {
        String id = UUID.randomUUID().toString().replace("-", "");
        claims = new LinkedHashMap<>(Map.of("sub", id, "id", id, "email", id+"@example.com", "email_verified", true,
                "given_name", "Dara", "family_name", "Sok", "name", "Dara Sok", "first_name", "Dara", "last_name", "Sok"));
        claims.put("roles", List.of("ADMIN")); claims.put("permissions", List.of("user:delete"));
        failure = ""; receivedVerifier = false;
    }
    record Flow(MockHttpSession session, String provider, String state, String location) {}
    Flow start(String provider, MockHttpSession session, boolean link) throws Exception {
        var request = link ? post("/profile/accounts/link/" + provider).with(csrf()) : get("/oauth2/authorization/" + provider);
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        String location = result.getResponse().getRedirectedUrl();
        var parameters = query(location);
        nonce = parameters.get("nonce");
        return new Flow((MockHttpSession)result.getRequest().getSession(false), provider, parameters.get("state"), location);
    }
    MvcResult callback(Flow flow) throws Exception {
        return mvc.perform(get("/login/oauth2/code/"+flow.provider()).session(flow.session()).param("state", flow.state()).param("code", "valid-code")).andReturn();
    }
    Map<String, String> query(String location) {
        var result = new HashMap<String, String>();
        for (String part : URI.create(location).getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            result.put(pair[0], URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }
    IamUserPrincipal principal(MockHttpSession session) {
        var context = (SecurityContext)session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(context); assertNotNull(context.getAuthentication());
        return assertInstanceOf(IamUserPrincipal.class, context.getAuthentication().getPrincipal());
    }
    void anonymous(MockHttpSession session) {
        var context = (SecurityContext)session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertTrue(context == null || context.getAuthentication() == null);
    }
    UserResult localUser() {
        String id = UUID.randomUUID().toString().replace("-", "");
        return use(RegisterUserUseCase.class).execute(new RegisterUserCommand("u"+id, id+"@example.com", PASSWORD, PASSWORD, "Dara", "Sok", "en"));
    }
    MockHttpSession login(UserResult user) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username", user.username()).param("password", PASSWORD))
                .andReturn().getRequest().getSession(false);
    }
    @ParameterizedTest @ValueSource(strings={"google", "github", "facebook", "telegram"})
    void validCallbacksProvisionAnInternalCredentialFreeUser(String provider) throws Exception {
        var flow = start(provider, null, false);
        assertEquals("/", callback(flow).getResponse().getRedirectedUrl());
        var principal = principal(flow.session());
        assertNull(principal.getPassword()); assertTrue(principal.isEnabled());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertFalse(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("user:delete")));
        var identities = use(ListUserIdentitiesUseCase.class).execute(principal.uuid());
        assertEquals(1, identities.size()); assertEquals(provider.toUpperCase(), identities.get(0).provider().name());
        assertEquals(Set.of("google", "telegram").contains(provider), identities.get(0).emailVerified());
        if (!provider.equals("facebook")) assertTrue(receivedVerifier);
        var page = mvc.perform(get("/profile/accounts").session(flow.session())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Linked accounts"))).andReturn();
        preview("accounts.html", page.getResponse().getContentAsString());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.CsvSource({"google,signature", "google,nonce", "google,audience", "google,issuer", "google,token", "telegram,signature", "telegram,nonce", "telegram,audience", "telegram,issuer", "telegram,token"})
    void invalidProviderProofCannotCreateAnIamSession(String provider, String invalid) throws Exception {
        failure = invalid;
        var flow = start(provider, null, false);
        assertEquals("/login?oauth2error", callback(flow).getResponse().getRedirectedUrl());
        anonymous(flow.session());
        assertNull(flow.session().getAttribute(FederationLoginHandler.PENDING));
    }
    @Test void stateMismatchAndCallbackReplayAreRejected() throws Exception {
        var flow = start("github", null, false);
        mvc.perform(get("/login/oauth2/code/github").session(flow.session()).param("state", "attacker").param("code", "valid-code"))
                .andExpect(redirectedUrl("/login?oauth2error"));
        anonymous(flow.session());
        assertEquals("/", callback(flow).getResponse().getRedirectedUrl());
        assertEquals("/login?oauth2error", callback(flow).getResponse().getRedirectedUrl());
    }
    @Test void missingProfileUsesExpiringSessionBoundCompletionAndUnverifiedEmail() throws Exception {
        claims.remove("email"); claims.remove("given_name"); claims.remove("family_name");
        var flow = start("telegram", null, false);
        assertEquals("/federation/complete", callback(flow).getResponse().getRedirectedUrl());
        anonymous(flow.session());
        var pending = (FederationLoginHandler.Pending)flow.session().getAttribute(FederationLoginHandler.PENDING);
        assertNotNull(pending);
        var page = mvc.perform(get("/federation/complete").session(flow.session())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Complete your profile"))).andReturn();
        preview("complete.html", page.getResponse().getContentAsString());
        mvc.perform(post("/federation/complete").session(flow.session()).param("flow", pending.flow())).andExpect(status().isForbidden());
        mvc.perform(post("/federation/complete").session(flow.session()).with(csrf()).param("flow", pending.flow())
                .param("email", "bad").param("givenName", "Dara").param("familyName", "Sok")).andExpect(status().isBadRequest());
        anonymous(flow.session());
        mvc.perform(post("/federation/complete").session(flow.session()).with(csrf()).param("flow", pending.flow())
                .param("email", UUID.randomUUID()+"@example.com").param("givenName", "Dara").param("familyName", "Sok"))
                .andExpect(redirectedUrl("/"));
        var principal = principal(flow.session());
        assertFalse(use(GetUserUseCase.class).execute(principal.uuid()).emailVerified());
        assertNull(flow.session().getAttribute(FederationLoginHandler.PENDING));
        mvc.perform(post("/federation/complete").session(flow.session()).with(csrf()).param("flow", pending.flow()))
                .andExpect(redirectedUrl("/login?oauth2error"));
    }
    @Test void expiredCompletionAndWrongFlowCannotProvision() throws Exception {
        claims.remove("given_name");
        var flow = start("google", null, false); callback(flow);
        var pending = (FederationLoginHandler.Pending)flow.session().getAttribute(FederationLoginHandler.PENDING);
        mvc.perform(post("/federation/complete").session(flow.session()).with(csrf()).param("flow", "another-tab"))
                .andExpect(redirectedUrl("/login?oauth2error"));
        flow.session().setAttribute(FederationLoginHandler.PENDING,
                new FederationLoginHandler.Pending(pending.identity(), Instant.now().minusSeconds(1), pending.flow()));
        mvc.perform(get("/federation/complete").session(flow.session())).andExpect(redirectedUrl("/login?oauth2error"));
        assertNull(flow.session().getAttribute(FederationLoginHandler.PENDING));
        anonymous(flow.session());
    }
    @Test void matchingEmailRequiresExplicitLinkAndLinkKeepsTheInternalOwner() throws Exception {
        var owner = localUser(); claims.put("email", owner.email());
        var ordinary = start("google", null, false);
        assertEquals("/login?oauth2error", callback(ordinary).getResponse().getRedirectedUrl());
        anonymous(ordinary.session());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).isEmpty());
        var session = login(owner);
        mvc.perform(post("/profile/accounts/link/google").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/profile/accounts/link/google").session(session)).andExpect(status().isMethodNotAllowed());
        var linking = start("google", session, true);
        assertEquals("/profile/accounts?linked", callback(linking).getResponse().getRedirectedUrl());
        assertEquals(owner.uuid(), principal(session).uuid());
        assertEquals(1, use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).size());
        var login = start("google", null, false); callback(login);
        assertEquals(owner.uuid(), principal(login.session()).uuid());
    }
    @Test void linkingRejectsAChangedOwnerAndCannotTransferAnIdentity() throws Exception {
        var owner = localUser(); var another = localUser();
        var session = login(owner); var linking = start("github", session, true);
        var anotherSession = login(another);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                anotherSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertEquals("/login?oauth2error", callback(linking).getResponse().getRedirectedUrl());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).isEmpty());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(another.uuid()).isEmpty());
        var ordinary = start("github", null, false); callback(ordinary);
        var externalOwner = principal(ordinary.session()).uuid();
        var conflict = start("github", login(owner), true);
        assertEquals("/profile/accounts?error", callback(conflict).getResponse().getRedirectedUrl());
        assertEquals(owner.uuid(), principal(conflict.session()).uuid());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).isEmpty());
        assertEquals(1, use(ListUserIdentitiesUseCase.class).execute(externalOwner).size());
    }
    @Test void disabledAndUnlinkedUsersCannotReturnThroughTheirProvider() throws Exception {
        var flow = start("github", null, false); callback(flow);
        var user = principal(flow.session());
        use(DisableUserUseCase.class).execute(user.uuid());
        var retry = start("github", null, false);
        assertEquals("/login?oauth2error", callback(retry).getResponse().getRedirectedUrl()); anonymous(retry.session());
    }
    @Test void socialSessionCanIssueInternalUuidTokensWithCurrentPermissions() throws Exception {
        var flow = start("google", null, false); callback(flow);
        var user = principal(flow.session());
        var clients = use(org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository.class);
        String clientId = "social-test-" + UUID.randomUUID();
        var client = org.springframework.security.oauth2.server.authorization.client.RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId).clientSecret(use(org.springframework.security.crypto.password.PasswordEncoder.class).encode("social-test-secret"))
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/social-callback").scope("openid").scope("email")
                .clientSettings(org.springframework.security.oauth2.server.authorization.settings.ClientSettings.builder()
                        .requireAuthorizationConsent(false).requireProofKey(true).build()).build();
        clients.save(client);
        String verifier = UUID.randomUUID().toString()+UUID.randomUUID();
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        var authorization = mvc.perform(get("/oauth2/authorize").session(flow.session()).queryParam("response_type", "code")
                .queryParam("client_id", clientId).queryParam("redirect_uri", "http://localhost/social-callback")
                .queryParam("scope", "openid email").queryParam("state", "local-state").queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String code = query(authorization.getResponse().getRedirectedUrl()).get("code"); assertNotNull(code);
        var exchange = mvc.perform(post("/oauth2/token").with(httpBasic(clientId, "social-test-secret"))
                .param("grant_type", "authorization_code").param("code", code).param("redirect_uri", "http://localhost/social-callback")
                .param("code_verifier", verifier)).andExpect(status().isOk()).andReturn();
        var tokens = JSON.readTree(exchange.getResponse().getContentAsString());
        var decoder = use(org.springframework.security.oauth2.jwt.JwtDecoder.class);
        var access = decoder.decode(tokens.get("access_token").asText());
        assertEquals(user.uuid().toString(), access.getSubject());
        assertEquals(user.uuid().toString(), decoder.decode(tokens.get("id_token").asText()).getSubject());
        assertFalse(access.getClaimAsStringList("authorities").contains("ROLE_ADMIN"));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + tokens.get("access_token").asText()))
                .andExpect(status().isOk());
    }
    @Test void failedLinkCallbackRestoresItsOwnerAndUnlinkedIdentityStaysReserved() throws Exception {
        var owner = localUser();
        var session = login(owner);
        var rejected = start("github", session, true);
        mvc.perform(get("/login/oauth2/code/github").session(session).param("state", rejected.state()).param("error", "access_denied"))
                .andExpect(redirectedUrl("/profile/accounts?error"));
        assertEquals(owner.uuid(), principal(session).uuid());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).isEmpty());
        var linked = start("github", session, true); callback(linked);
        var identity = use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).get(0);
        use(UnlinkIdentityUseCase.class).execute(owner.uuid(), identity.uuid());
        var retry = start("github", null, false);
        assertEquals("/login?oauth2error", callback(retry).getResponse().getRedirectedUrl()); anonymous(retry.session());
        var relink = start("github", session, true); callback(relink);
        assertEquals(identity.uuid(), use(ListUserIdentitiesUseCase.class).execute(owner.uuid()).get(0).uuid());
    }
    @Test void completionCannotOverwriteSignedProviderFieldsAndCanBeCancelled() throws Exception {
        claims.remove("given_name");
        var flow = start("google", null, false); callback(flow);
        var pending = (FederationLoginHandler.Pending)flow.session().getAttribute(FederationLoginHandler.PENDING);
        String signedEmail = claims.get("email").toString();
        mvc.perform(post("/federation/complete").session(flow.session()).with(csrf()).param("flow", pending.flow())
                .param("givenName", "Dara").param("familyName", "Attacker").param("email", "attacker@example.com"))
                .andExpect(redirectedUrl("/"));
        var user = use(GetUserUseCase.class).execute(principal(flow.session()).uuid());
        assertEquals(signedEmail, user.email()); assertTrue(user.emailVerified()); assertEquals("Sok", user.profile().familyName());
        profile(); claims.remove("given_name");
        var cancel = start("google", null, false); callback(cancel);
        mvc.perform(post("/federation/cancel").session(cancel.session()).with(csrf())).andExpect(redirectedUrl("/login"));
        assertNull(cancel.session().getAttribute(FederationLoginHandler.PENDING)); anonymous(cancel.session());
    }

}
