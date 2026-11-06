package istad.ha.iamservice;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.infrastructure.init.InitRegisterClient;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Protocol-level tests use actual endpoints, signed tokens and the production JDBC services. */
class AuthorizationServerIntegrationTests extends IamIntegrationTestSupport {
    static final String PASSWORD = "Str0ng!Pass2026";
    static final String CLIENT_SECRET = "test-code-client-secret";
    static final String CALLBACK = "http://localhost/test/callback";
    static final Set<String> OIDC_SCOPES = Set.of("openid", "profile", "email", "phone", "address");
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext beans;
    @Autowired JdbcTemplate jdbc;
    @Autowired RegisteredClientRepository clients;
    @Autowired OAuth2AuthorizationService authorizations;
    @Autowired OAuth2AuthorizationConsentService consents;
    @Autowired JwtDecoder decoder;
    @Autowired PasswordEncoder encoder;
    final JsonMapper json = new JsonMapper();
    <T> T use(Class<T> type) { return beans.getBean(type); }
    String unique() { return UUID.randomUUID().toString().replace("-", ""); }
    UserResult user() {
        String id = unique();
        var result = use(RegisterUserUseCase.class).execute(new RegisterUserCommand("u"+id, id+"@example.com", PASSWORD, PASSWORD, "Dara", "Sok", "km"));
        return use(UpdateUserProfileUseCase.class).execute(new UpdateUserProfileCommand(result.uuid(), ProfilePatch.builder()
                .middleName("Middle").nickname("D").gender("male").birthdate(LocalDate.of(2003,5,20))
                .profileUrl("https://example.com/profile").pictureUrl("https://example.com/avatar")
                .websiteUrl("https://example.com").zoneinfo("Asia/Phnom_Penh")
                .phoneNumber("+855"+Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000L + 1000000000L))
                .address(new Address("No.24 St.562", "Toul Kork", "Phnom Penh", "120408", "KH")).build()));
    }
    RegisteredClient client(boolean consent) {
        var client = RegisteredClient.withId(unique()).clientId("code_"+unique()).clientName("Test OIDC client")
                .clientSecret(encoder.encode(CLIENT_SECRET)).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(CALLBACK).scopes(scopes -> scopes.addAll(OIDC_SCOPES))
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(consent).build())
                .tokenSettings(TokenSettings.builder().reuseRefreshTokens(false).build()).build();
        clients.save(client); return client;
    }
    MockHttpSession login(UserResult user) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username", user.username()).param("password", PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }
    record Code(String value, String verifier, RegisteredClient client) {}
    Code code(UserResult user, RegisteredClient client, Set<String> scopes) throws Exception {
        var session = login(user); String verifier = unique()+unique(); String state = unique();
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        var response = mvc.perform(get("/oauth2/authorize").session(session).queryParam("response_type", "code")
                .queryParam("client_id", client.getClientId()).queryParam("redirect_uri", CALLBACK)
                .queryParam("scope", String.join(" ", scopes)).queryParam("state", state).queryParam("nonce", unique())
                .queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256")).andReturn().getResponse();
        if (response.getStatus() == 302 && response.getHeader("Location").contains("/oauth2/consent?")) {
            response = mvc.perform(get(URI.create(response.getHeader("Location"))).session(session)).andExpect(status().isOk()).andReturn().getResponse();
        }
        if (response.getStatus() == 200) {
            var match = Pattern.compile("name=\"state\"[^>]*value=\"([^\"]+)\"").matcher(response.getContentAsString());
            assertTrue(match.find(), "Consent response must contain state");
            response = mvc.perform(post("/oauth2/authorize").session(session).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("client_id", client.getClientId()).param("state", match.group(1))
                    .param("scope", scopes.stream().filter(scope -> !scope.equals("openid")).toArray(String[]::new)))
                    .andReturn().getResponse();
        }
        assertEquals(302, response.getStatus(), "Authorization must redirect with a code");
        String location = response.getHeader("Location"); assertNotNull(location); assertTrue(location.startsWith(CALLBACK), location);
        String code = Arrays.stream(URI.create(location).getRawQuery().split("&")).filter(pair -> pair.startsWith("code="))
                .map(pair -> URLDecoder.decode(pair.substring(5), StandardCharsets.UTF_8)).findFirst().orElseThrow();
        return new Code(code, verifier, client);
    }
    JsonNode body(ResultActions action) throws Exception { return json.readTree(action.andReturn().getResponse().getContentAsString()); }
    ResultActions exchange(Code code) throws Exception {
        return mvc.perform(post("/oauth2/token").with(httpBasic(code.client().getClientId(), CLIENT_SECRET))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "authorization_code")
                .param("redirect_uri", CALLBACK).param("code", code.value()).param("code_verifier", code.verifier()));
    }
    JsonNode tokens(UserResult user, RegisteredClient client, Set<String> scopes) throws Exception {
        return body(exchange(code(user, client, scopes)).andExpect(status().isOk()));
    }
    ResultActions refresh(RegisteredClient client, String refreshToken, String scope) throws Exception {
        var request = post("/oauth2/token").with(httpBasic(client.getClientId(), CLIENT_SECRET))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "refresh_token").param("refresh_token", refreshToken);
        if (scope != null) request.param("scope", scope);
        return mvc.perform(request);
    }
    @ParameterizedTest @ValueSource(strings={"openid", "openid profile", "openid email", "openid phone", "openid address", "openid profile email phone address"})
    void oidcClaimsAndUserInfoOnlyExposeAuthorizedScopes(String scope) throws Exception {
        var user = user(); var requested = new HashSet<>(List.of(scope.split(" ")));
        var tokens = tokens(user, client(false), requested);
        var id = decoder.decode(tokens.get("id_token").asText());
        var access = decoder.decode(tokens.get("access_token").asText());
        assertEquals(user.uuid().toString(), id.getSubject()); assertEquals(id.getSubject(), access.getSubject());
        assertEquals(requested.contains("profile"), id.hasClaim("given_name"));
        assertEquals(requested.contains("email"), id.hasClaim("email"));
        assertEquals(requested.contains("phone"), id.hasClaim("phone_number"));
        assertEquals(requested.contains("address"), id.hasClaim("address"));
        if (requested.contains("profile")) {
            assertEquals("Dara Sok", id.getClaimAsString("name")); assertEquals("2003-05-20", id.getClaimAsString("birthdate"));
            assertEquals(user.username(), id.getClaimAsString("preferred_username"));
            assertTrue(id.getClaims().keySet().containsAll(Set.of("name","given_name","family_name","middle_name","nickname",
                    "preferred_username","profile","picture","website","gender","birthdate","zoneinfo","locale","updated_at")));
        }
        if (requested.contains("email")) assertEquals(false, id.getClaimAsBoolean("email_verified"));
        if (requested.contains("phone")) assertEquals(false, id.getClaimAsBoolean("phone_number_verified"));
        if (requested.contains("address")) assertEquals("KH", id.getClaimAsMap("address").get("country"));
        assertTrue(access.getClaimAsStringList("roles").contains("USER"));
        assertTrue(access.getClaimAsStringList("permissions").contains("user:read"));
        assertTrue(access.getClaimAsStringList("authorities").contains("ROLE_USER"));
        assertFalse(access.hasClaim("email")); assertFalse(id.hasClaim("permissions"));
        var info = body(mvc.perform(get("/userinfo").header("Authorization", "Bearer "+tokens.get("access_token").asText()))
                .andExpect(status().isOk()));
        assertEquals(user.uuid().toString(), info.get("sub").asText());
        assertEquals(requested.contains("profile"), info.has("given_name"));
        assertEquals(requested.contains("email"), info.has("email"));
        assertEquals(requested.contains("phone"), info.has("phone_number"));
        assertEquals(requested.contains("address"), info.has("address"));
        for (String sensitive : List.of("password", "passwordHash", "attributes", "roles", "permissions", "authorities")) assertFalse(info.has(sensitive));
    }
    @Test void jdbcServicesReloadCodesTokensAndCredentialFreePrincipals() throws Exception {
        assertInstanceOf(JdbcOAuth2AuthorizationService.class, authorizations);
        assertInstanceOf(JdbcOAuth2AuthorizationConsentService.class, consents);
        var user = user(); var code = code(user, client(false), Set.of("openid", "profile"));
        var reloaded = istad.ha.iamservice.infrastructure.config.JdbcAuthorizationPersistence.create(jdbc, clients);
        var saved = reloaded.findByToken(code.value(), new OAuth2TokenType("code")); assertNotNull(saved);
        var authentication = (org.springframework.security.core.Authentication)saved.getAttribute(Principal.class.getName());
        var principal = assertInstanceOf(IamUserPrincipal.class, authentication.getPrincipal());
        assertEquals(user.uuid(), principal.uuid()); assertNull(principal.getPassword());
        String stored = jdbc.queryForObject("select attributes from oauth2_authorization where id=?", String.class, saved.getId());
        assertNotNull(stored); assertFalse(stored.contains("$2a$")); assertFalse(stored.contains("$2b$")); assertFalse(stored.contains(PASSWORD));
        var tokens = body(exchange(code).andExpect(status().isOk()));
        assertNotNull(reloaded.findByToken(tokens.get("access_token").asText(), OAuth2TokenType.ACCESS_TOKEN));
        assertNotNull(reloaded.findByToken(tokens.get("refresh_token").asText(), OAuth2TokenType.REFRESH_TOKEN));
        assertFalse(reloaded.findByToken(code.value(), new OAuth2TokenType("code")).getToken(OAuth2AuthorizationCode.class).isActive());
    }
    @Test void consentSurvivesAServiceReconstructionAndContainsOnlyGrantedScopes() throws Exception {
        var user = user(); var client = client(true);
        tokens(user, client, Set.of("openid", "profile", "email"));
        var consent = new JdbcOAuth2AuthorizationConsentService(jdbc, clients).findById(client.getId(), user.username());
        assertNotNull(consent); assertTrue(consent.getScopes().containsAll(Set.of("profile", "email")));
        assertFalse(consent.getScopes().contains("phone"));
        assertEquals(1, jdbc.queryForObject("select count(*) from oauth2_authorization_consent where registered_client_id=? and principal_name=?",
                Integer.class, client.getId(), user.username()));
    }
    @Test void refreshReevaluatesPermissionsPreservesSubjectAndRotatesTokens() throws Exception {
        var user = user(); var client = client(false); var tokens = tokens(user, client, Set.of("openid", "profile", "email"));
        var permission = use(istad.ha.iamservice.application.port.PermissionRepository.class).findByName("user:read").orElseThrow();
        use(GrantPermissionsToUserUseCase.class).execute(new GrantPermissionsToUserCommand(user.uuid(), List.of(permission.uuid()), PermissionEffect.DENY, null, "Remove read"));
        String oldRefresh = tokens.get("refresh_token").asText();
        var updated = body(refresh(client, oldRefresh, "openid profile").andExpect(status().isOk()));
        var access = decoder.decode(updated.get("access_token").asText());
        assertEquals(user.uuid().toString(), access.getSubject());
        assertFalse(access.getClaimAsStringList("permissions").contains("user:read"));
        assertFalse(decoder.decode(updated.get("id_token").asText()).hasClaim("email"));
        assertNotEquals(oldRefresh, updated.get("refresh_token").asText());
        refresh(client, oldRefresh, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_grant"));
        use(DisableUserUseCase.class).execute(user.uuid());
        refresh(client, updated.get("refresh_token").asText(), null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }
    JsonNode serviceToken(String id, String secret, String scope) throws Exception {
        var request = post("/oauth2/token").with(httpBasic(id, secret)).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "client_credentials");
        if (scope != null) request.param("scope", scope);
        return body(mvc.perform(request).andExpect(status().isOk()));
    }
    @Test void serviceSubjectsAudiencesAndScopeSeparationRemainCompatible() throws Exception {
        var order = decoder.decode(serviceToken("order-service", "test-order-secret", "customer.read").get("access_token").asText());
        assertEquals("order-service", order.getSubject()); assertEquals(List.of("customer-service"), order.getAudience());
        var postman = decoder.decode(serviceToken("postman-client", "test-postman-only", "api.read").get("access_token").asText());
        assertEquals(List.of("order-service"), postman.getAudience());
        assertFalse(order.hasClaim("email")); assertFalse(order.hasClaim("permissions"));
        mvc.perform(get("/userinfo").header("Authorization", "Bearer "+order.getTokenValue())).andExpect(status().is4xxClientError());
    }
    Map<String,Object> registration() {
        return Map.of("client_name", "ISTAD E-Commerce Web", "redirect_uris", List.of("http://localhost:8081/login/oauth2/code/iam"),
                "grant_types", List.of("authorization_code", "refresh_token"), "response_types", List.of("code"),
                "scope", "openid profile email", "token_endpoint_auth_method", "client_secret_basic");
    }
    String initialToken() throws Exception { return serviceToken("client-registrar", "test-registration-secret", "client.create").get("access_token").asText(); }
    ResultActions register(String token, Object metadata) throws Exception {
        var request = post("/connect/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(metadata));
        if (token != null) request.header("Authorization", "Bearer "+token);
        return mvc.perform(request);
    }
    @Test void registrarIsSeededWithAnIndependentSecretAndStartupPreservesIt() {
        var client = clients.findByClientId("client-registrar"); assertNotNull(client);
        assertEquals(Set.of("client.create"), client.getScopes()); assertTrue(encoder.matches("test-registration-secret", client.getClientSecret()));
        use(InitRegisterClient.class).run(new org.springframework.boot.DefaultApplicationArguments());
        assertEquals(client.getId(), clients.findByClientId(client.getClientId()).getId());
        assertEquals(client.getClientSecret(), clients.findByClientId(client.getClientId()).getClientSecret());
    }
    @Test void dynamicRegistrationReturnsOneTimeSecretAndOwnRegistrationReadToken() throws Exception {
        String initial = initialToken();
        var result = body(register(initial, registration()).andExpect(status().isCreated()));
        String id = result.get("client_id").asText(); String secret = result.get("client_secret").asText();
        String readToken = result.get("registration_access_token").asText();
        String readUri = result.get("registration_client_uri").asText();
        assertEquals("http://localhost:9000/connect/register?client_id="+id, readUri);
        var persisted = clients.findByClientId(id); assertNotNull(persisted); assertTrue(encoder.matches(secret, persisted.getClientSecret()));
        assertNotEquals(secret, persisted.getClientSecret()); assertEquals(Set.of("openid", "profile", "email"), persisted.getScopes());
        assertTrue(persisted.getClientSettings().isRequireProofKey()); assertTrue(persisted.getClientSettings().isRequireAuthorizationConsent());
        mvc.perform(get(URI.create(readUri)).header("Authorization", "Bearer "+readToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.client_id").value(id)).andExpect(jsonPath("$.client_secret").doesNotExist());
        var saved = istad.ha.iamservice.infrastructure.config.JdbcAuthorizationPersistence.create(jdbc, clients).findByToken(readToken, OAuth2TokenType.ACCESS_TOKEN); assertNotNull(saved);
        assertTrue(saved.getAccessToken().isActive());
        register(initial, registration()).andExpect(status().isUnauthorized());
        mvc.perform(get("/connect/register").queryParam("client_id", "oidc-client").header("Authorization", "Bearer "+readToken))
                .andExpect(status().is4xxClientError());
        register(readToken, registration()).andExpect(status().isForbidden());
        mvc.perform(get("/.well-known/openid-configuration")).andExpect(status().isOk())
                .andExpect(jsonPath("$.registration_endpoint").value("http://localhost:9000/connect/register"));
    }
    @Test void dynamicRegistrationRejectsMissingWrongAndUnpersistedTokens() throws Exception {
        register(null, registration()).andExpect(status().isUnauthorized());
        register("invalid-token", registration()).andExpect(status().isUnauthorized());
        String wrong = serviceToken("postman-client", "test-postman-only", "api.read").get("access_token").asText();
        register(wrong, registration()).andExpect(status().isForbidden());
        mvc.perform(get("/connect/register").queryParam("client_id", "oidc-client").header("Authorization", "Bearer "+initialToken()))
                .andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings={"client.create", "client.read", "api.read", "user:assign", "openid profile client.create"})
    void dynamicRegistrationCannotMintPrivilegedScopes(String scope) throws Exception {
        var metadata = new HashMap<>(registration()); metadata.put("scope", scope);
        register(initialToken(), metadata).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_scope"));
    }
    @Test void registrationValidatesRedirectsGrantsAndAuthenticationMetadata() throws Exception {
        for (String redirect : List.of("javascript:alert(1)", "//evil.example/callback", "https://example.com/cb#fragment")) {
            var metadata = new HashMap<>(registration()); metadata.put("redirect_uris", List.of(redirect));
            register(initialToken(), metadata).andExpect(status().isBadRequest());
        }
        var metadata = new HashMap<>(registration()); metadata.put("grant_types", List.of("client_credentials"));
        register(initialToken(), metadata).andExpect(status().isBadRequest());
        metadata = new HashMap<>(registration()); metadata.put("token_endpoint_auth_method", "none");
        register(initialToken(), metadata).andExpect(status().isBadRequest());
    }
}
