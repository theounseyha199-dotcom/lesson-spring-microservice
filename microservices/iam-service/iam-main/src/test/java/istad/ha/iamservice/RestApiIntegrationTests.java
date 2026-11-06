package istad.ha.iamservice;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Real RSA tokens pass through the production API filter chain and real database-backed use cases. */
class RestApiIntegrationTests extends IamIntegrationTestSupport {
    static final String PASSWORD = "Str0ng!Pass2026";
    static final String ISSUER = "http://localhost:9000";
    static final UUID MISSING = UUID.fromString("54eef172-30ed-4c89-a175-c8e6b7795400");
    static final List<String> ADMIN = List.of("user:create", "user:read", "user:update", "user:delete", "user:assign",
            "role:create", "role:read", "role:update", "role:delete", "role:assign",
            "permission:create", "permission:read", "permission:update", "permission:delete");
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext beans;
    @Autowired JWKSource<SecurityContext> keys;
    final JsonMapper json = new JsonMapper();
    <T> T use(Class<T> type) { return beans.getBean(type); }
    String unique() { return UUID.randomUUID().toString().replace("-", ""); }
    String token(UUID subject, Collection<String> authorities) { return token(subject.toString(), authorities, ISSUER, Instant.now().plusSeconds(300), false); }
    String token(String subject, Collection<String> authorities, String issuer, Instant expires, boolean idToken) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject).issuedAt(expires.minusSeconds(600))
                .expiresAt(expires).audience(List.of("iam-service")).claim("authorities", authorities);
        if (idToken) claims.claim("nonce", "id-token-nonce"); else claims.claim("scope", "openid profile");
        return new NimbusJwtEncoder(keys).encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims.build())).getTokenValue();
    }
    UserResult user() {
        String id = unique();
        return use(RegisterUserUseCase.class).execute(new RegisterUserCommand("u" + id, id + "@example.com", PASSWORD, PASSWORD, "Dara", "Sok", "en"));
    }
    String admin() { return token(user().uuid(), ADMIN); }
    ResultActions call(String method, String path, String token, Object body) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body instanceof String s ? s : json.writeValueAsString(body));
        return mvc.perform(request);
    }
    JsonNode body(ResultActions action) throws Exception { return json.readTree(action.andReturn().getResponse().getContentAsString()); }
    Map<String,Object> registration() {
        String id = unique();
        return Map.of("username", "u" + id, "email", id + "@example.com", "password", PASSWORD,
                "confirmPassword", PASSWORD, "givenName", "Dara", "familyName", "Sok", "locale", "km");
    }
    @Test void publicRegistrationMatchesTheResponseContractAndCannotInjectPrivileges() throws Exception {
        var request = registration();
        var response = call("POST", "/api/auth/register", null, request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.locale").value("km")).andExpect(jsonPath("$.roles[0].name").value("USER"))
                .andExpect(jsonPath("$.roles[0].active").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.profile").doesNotExist());
        assertEquals(Set.of("uuid", "username", "email", "emailVerified", "name", "locale", "enabled", "roles", "createdAt", "updatedAt"), body(response).propertyNames());
        call("POST", "/api/auth/register", null, request).andExpect(status().isConflict());
        var injected = new HashMap<>(registration()); injected.put("roleUuids", List.of(MISSING));
        call("POST", "/api/auth/register", null, injected).andExpect(status().isBadRequest());
    }
    @Test void validationAndParserErrorsAreProblemDetailsWithoutSensitiveValues() throws Exception {
        var invalid = new HashMap<>(registration()); invalid.put("email", "not-an-email");
        call("POST", "/api/auth/register", null, invalid).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://iam.istad.co/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        String privateValue = "private-password-marker";
        var malformed = call("POST", "/api/auth/register", null, "{\"password\":\"" + privateValue + "\"")
                .andExpect(status().isBadRequest());
        assertFalse(malformed.andReturn().getResponse().getContentAsString().contains(privateValue));
        var mismatch = new HashMap<>(registration()); mismatch.put("confirmPassword", "different");
        call("POST", "/api/auth/register", null, mismatch).andExpect(status().isBadRequest());
        var bytes = new HashMap<>(registration()); bytes.put("password", "ខ".repeat(30)); bytes.put("confirmPassword", "ខ".repeat(30));
        call("POST", "/api/auth/register", null, bytes).andExpect(status().isBadRequest());
    }
    @Test void missingInvalidExpiredForeignAndIdTokensAreRejectedAsJson401() throws Exception {
        var user = user();
        for (String value : Arrays.asList(null, "invalid-token", token(user.uuid().toString(), ADMIN, ISSUER, Instant.now().minusSeconds(300), false),
                token(user.uuid().toString(), ADMIN, "https://foreign.example", Instant.now().plusSeconds(300), false),
                token(user.uuid().toString(), ADMIN, ISSUER, Instant.now().plusSeconds(300), true))) {
            call("GET", "/api/me", value, null).andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        String valid = token(user.uuid(), ADMIN);
        String[] segments = valid.split("\\.");
        segments[2] = (segments[2].charAt(0) == 'A' ? "B" : "A") + segments[2].substring(1);
        call("GET", "/api/me", String.join(".", segments), null).andExpect(status().isUnauthorized());
    }
    @Test void browserSessionsDoNotAuthorizeApiAndBrowserCsrfRemainsEnabled() throws Exception {
        var user = user();
        mvc.perform(post("/login").param("username", user.username()).param("password", PASSWORD))
                .andExpect(status().isForbidden());
        var session = mvc.perform(post("/login").with(csrf()).param("username", user.username()).param("password", PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/me").session((org.springframework.mock.web.MockHttpSession)session))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
    @Test void authorizationCodeTokenUsesTheInternalSubjectAndCanCallMe() throws Exception {
        var user = user();
        String clientId = "api_test_"+unique(); String redirect = "http://localhost/test/callback";
        var client = org.springframework.security.oauth2.server.authorization.client.RegisteredClient.withId(unique())
                .clientId(clientId).clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirect).scope("openid").scope("profile")
                .clientSettings(org.springframework.security.oauth2.server.authorization.settings.ClientSettings.builder()
                        .requireProofKey(true).requireAuthorizationConsent(false).build()).build();
        use(org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository.class).save(client);
        var session = (org.springframework.mock.web.MockHttpSession)mvc.perform(post("/login").with(csrf())
                .param("username", user.username()).param("password", PASSWORD)).andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
        String verifier = unique()+unique();
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        String location = mvc.perform(get("/oauth2/authorize").session(session)
                .queryParam("response_type", "code").queryParam("client_id", clientId).queryParam("redirect_uri", redirect)
                .queryParam("scope", "openid profile").queryParam("state", "test-state")
                .queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location); assertTrue(location.startsWith(redirect), location);
        String code = Arrays.stream(java.net.URI.create(location).getQuery().split("&"))
                .filter(pair -> pair.startsWith("code=")).map(pair -> java.net.URLDecoder.decode(pair.substring(5), java.nio.charset.StandardCharsets.UTF_8))
                .findFirst().orElseThrow();
        var response = body(mvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "authorization_code")
                .param("client_id", clientId).param("redirect_uri", redirect).param("code", code).param("code_verifier", verifier))
                .andExpect(status().isOk()));
        String access = response.get("access_token").asText();
        call("GET", "/api/me", access, null).andExpect(status().isOk()).andExpect(jsonPath("$.uuid").value(user.uuid().toString()));
        call("GET", "/api/users", access, null).andExpect(status().isOk());
        call("GET", "/api/me", response.get("id_token").asText(), null).andExpect(status().isUnauthorized());
    }
    record Route(String method, String path, String permission, Object request) {}
    static Stream<Route> protectedRoutes() {
        var routes = new ArrayList<Route>(); String id = MISSING.toString();
        Object createUser = Map.of("username", "route-user", "email", "route@example.com", "password", PASSWORD, "givenName", "Dara", "familyName", "Sok");
        routes.add(new Route("POST", "/api/users", "user:create", createUser));
        routes.add(new Route("GET", "/api/users", "user:read", null));
        routes.add(new Route("GET", "/api/users/"+id, "user:read", null));
        routes.add(new Route("PATCH", "/api/users/"+id, "user:update", Map.of("nickname", "D")));
        routes.add(new Route("DELETE", "/api/users/"+id, "user:delete", null));
        for (String action : List.of("enable", "disable", "lock", "unlock")) routes.add(new Route("POST", "/api/users/"+id+"/"+action, "user:update", null));
        routes.add(new Route("PUT", "/api/users/"+id+"/password", "user:update", Map.of("newPassword", PASSWORD, "confirmPassword", PASSWORD)));
        routes.add(new Route("GET", "/api/users/"+id+"/authorities", "user:read", null));
        routes.add(new Route("POST", "/api/users/"+id+"/roles", "user:assign", Map.of("roleUuids", List.of(id))));
        routes.add(new Route("DELETE", "/api/users/"+id+"/roles/"+id, "user:assign", Map.of("reason", "Finished")));
        routes.add(new Route("POST", "/api/users/"+id+"/permissions", "user:assign", Map.of("permissionUuids", List.of(id), "effect", "GRANT")));
        routes.add(new Route("DELETE", "/api/users/"+id+"/permissions/"+id, "user:assign", Map.of("reason", "Finished")));
        routes.add(new Route("GET", "/api/users/"+id+"/identities", "user:read", null));
        routes.add(new Route("DELETE", "/api/users/"+id+"/identities/"+id, "user:update", null));
        routes.add(new Route("POST", "/api/roles", "role:create", Map.of("name", "ROUTE_TEST")));
        routes.add(new Route("GET", "/api/roles", "role:read", null));
        routes.add(new Route("GET", "/api/roles/"+id, "role:read", null));
        routes.add(new Route("PATCH", "/api/roles/"+id, "role:update", Map.of("description", "test")));
        routes.add(new Route("DELETE", "/api/roles/"+id, "role:delete", null));
        for (String action : List.of("activate", "deactivate")) routes.add(new Route("POST", "/api/roles/"+id+"/"+action, "role:update", null));
        routes.add(new Route("POST", "/api/roles/"+id+"/permissions", "role:assign", Map.of("permissionUuids", List.of(id))));
        routes.add(new Route("DELETE", "/api/roles/"+id+"/permissions/"+id, "role:assign", Map.of("reason", "Finished")));
        routes.add(new Route("POST", "/api/permissions", "permission:create", Map.of("resource", "test", "action", "read")));
        routes.add(new Route("GET", "/api/permissions", "permission:read", null));
        routes.add(new Route("GET", "/api/permissions/"+id, "permission:read", null));
        routes.add(new Route("PATCH", "/api/permissions/"+id, "permission:update", Map.of("description", "test")));
        routes.add(new Route("DELETE", "/api/permissions/"+id, "permission:delete", null));
        for (String action : List.of("activate", "deactivate")) routes.add(new Route("POST", "/api/permissions/"+id+"/"+action, "permission:update", null));
        return routes.stream();
    }
    @ParameterizedTest @MethodSource("protectedRoutes")
    void everyAdministrativeRouteRequiresItsExactPermission(Route route) throws Exception {
        var id = user().uuid();
        call(route.method(), route.path(), null, route.request()).andExpect(status().isUnauthorized());
        call(route.method(), route.path(), token(id, List.of("ROLE_ADMIN", route.permission()+"_wrong")), route.request())
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        // These UUIDs do not exist: exact permission reaches the use case, rather than another 403.
        if (route.path().contains(MISSING.toString())) {
            call(route.method(), route.path(), token(id, List.of(route.permission())), route.request()).andExpect(status().isNotFound());
        }
    }
    @Test void meAlwaysUsesTheVerifiedSubjectAndPatchPreservesAbsentFields() throws Exception {
        var one = user(); var two = user(); String token = token(one.uuid(), List.of());
        call("GET", "/api/me", token, null).andExpect(status().isOk()).andExpect(jsonPath("$.uuid").value(one.uuid().toString()));
        call("PATCH", "/api/me", token, Map.of("nickname", "D", "locale", "km")).andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("km")).andExpect(jsonPath("$.name").value("Dara Sok"));
        call("PATCH", "/api/me", token, "{\"nickname\":null}").andExpect(status().isOk());
        assertNull(use(GetUserUseCase.class).execute(one.uuid()).profile().nickname());
        assertEquals("en", use(GetUserUseCase.class).execute(two.uuid()).profile().locale());
        call("PATCH", "/api/me", token, Map.of("userUuid", two.uuid(), "emailVerified", true)).andExpect(status().isBadRequest());
        call("PATCH", "/api/me", token, "{\"givenName\":null}").andExpect(status().isBadRequest());
        call("GET", "/api/me", token("postman-client", List.of(), ISSUER, Instant.now().plusSeconds(300), false), null).andExpect(status().isForbidden());
        call("GET", "/api/users/"+two.uuid(), token, null).andExpect(status().isForbidden());
    }
    @Test void mePasswordUsesCurrentPasswordAndNeverChangesAnotherUser() throws Exception {
        var one = user(); var two = user(); String token = token(one.uuid(), List.of());
        var password = Map.of("currentPassword", "wrong", "newPassword", "N3w!Pass2026!", "confirmPassword", "N3w!Pass2026!");
        call("PUT", "/api/me/password", token, password).andExpect(status().isBadRequest());
        password = Map.of("currentPassword", PASSWORD, "newPassword", "N3w!Pass2026!", "confirmPassword", "N3w!Pass2026!");
        call("PUT", "/api/me/password", token, password).andExpect(status().isNoContent());
        var hasher = use(istad.ha.iamservice.application.port.PasswordHasher.class);
        assertTrue(hasher.matches("N3w!Pass2026!", use(LoadUserForAuthenticationUseCase.class).execute(one.username()).passwordHash()));
        assertTrue(hasher.matches(PASSWORD, use(LoadUserForAuthenticationUseCase.class).execute(two.username()).passwordHash()));
    }
    FederatedIdentityCommand provider(String subject) {
        return new FederatedIdentityCommand(Provider.GOOGLE, subject, "provider@example.com", true, "Dara Sok", "dara",
                null, Map.of("private", "provider-secret-marker"), "Dara", "Sok", "en");
    }
    @Test void identitiesAreSafeAndSelfServiceCannotUnlinkAnotherOwnersIdentity() throws Exception {
        var one = user(); var two = user();
        var proof = provider(unique());
        var identity = use(LinkIdentityUseCase.class).execute(one.uuid(), proof);
        String token = token(one.uuid(), List.of());
        var response = call("GET", "/api/me/identities", token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("GOOGLE")).andExpect(jsonPath("$[0].attributes").doesNotExist());
        assertFalse(response.andReturn().getResponse().getContentAsString().contains("provider-secret-marker"));
        call("DELETE", "/api/me/identities/"+identity.uuid(), token(two.uuid(), List.of()), null).andExpect(status().isNotFound());
        call("GET", "/api/users/"+one.uuid()+"/identities", admin(), null).andExpect(status().isOk());
        call("DELETE", "/api/me/identities/"+identity.uuid(), token, null).andExpect(status().isNoContent());
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(one.uuid()).isEmpty());
        var again = use(LinkIdentityUseCase.class).execute(one.uuid(), proof);
        call("DELETE", "/api/users/"+one.uuid()+"/identities/"+again.uuid(), admin(), null).andExpect(status().isNoContent());
    }
    @Test void administratorsCanCreatePatchResetAndChangeAllAccountFlags() throws Exception {
        String admin = admin(); var input = new HashMap<>(registration()); input.remove("confirmPassword");
        input.put("address", Map.of("country", "KH", "locality", "Phnom Penh"));
        UUID id = UUID.fromString(body(call("POST", "/api/users", admin, input).andExpect(status().isCreated())).get("uuid").asText());
        String path = "/api/users/"+id;
        call("GET", path, admin, null).andExpect(status().isOk());
        call("PATCH", path, admin, Map.of("givenName", "Updated")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Sok"));
        for (String action : List.of("disable", "enable", "lock", "unlock")) {
            call("POST", path+"/"+action, admin, null).andExpect(status().isNoContent());
            var user = use(GetUserUseCase.class).execute(id);
            if (action.equals("disable") || action.equals("enable")) assertEquals(action.equals("enable"), user.enabled());
            else assertEquals(action.equals("unlock"), user.accountNonLocked());
        }
        call("PUT", path+"/password", admin, Map.of("newPassword", "Reset!Pass2026", "confirmPassword", "Reset!Pass2026"))
                .andExpect(status().isNoContent());
        assertTrue(use(istad.ha.iamservice.application.port.PasswordHasher.class).matches("Reset!Pass2026", use(LoadUserForAuthenticationUseCase.class).execute((String)input.get("username")).passwordHash()));
        call("DELETE", path, admin, null).andExpect(status().isNoContent());
        call("GET", path, admin, null).andExpect(status().isNotFound());
    }
    @Test void rolePermissionLifecycleAssignmentsDenialsAndResponseShapesWork() throws Exception {
        String admin = admin(); var user = user(); String resource = "test_"+unique();
        UUID permission = UUID.fromString(body(call("POST", "/api/permissions", admin,
                Map.of("resource", resource, "action", "read", "description", "Read reports")).andExpect(status().isCreated())).get("uuid").asText());
        String pp = "/api/permissions/"+permission;
        call("GET", pp, admin, null).andExpect(status().isOk());
        call("PATCH", pp, admin, "{\"description\":null}").andExpect(status().isOk());
        for (String action : List.of("deactivate", "activate")) call("POST", pp+"/"+action, admin, null).andExpect(status().isNoContent());
        UUID role = UUID.fromString(body(call("POST", "/api/roles", admin,
                Map.of("name", "R"+unique(), "permissionUuids", List.of(permission))).andExpect(status().isCreated())).get("uuid").asText());
        String rp = "/api/roles/"+role; String up = "/api/users/"+user.uuid();
        call("GET", rp, admin, null).andExpect(status().isOk());
        call("PATCH", rp, admin, Map.of("description", "Updated")).andExpect(status().isOk());
        for (String action : List.of("deactivate", "activate")) call("POST", rp+"/"+action, admin, null).andExpect(status().isNoContent());
        call("POST", up+"/roles", admin, Map.of("roleUuids", List.of(role), "expiresAt", Instant.now().plusSeconds(3600).toString(), "reason", "Temporary"))
                .andExpect(status().isOk());
        call("POST", up+"/roles", admin, Map.of("roleUuids", List.of(role))).andExpect(status().isConflict());
        call("POST", up+"/permissions", admin, Map.of("permissionUuids", List.of(permission), "effect", "DENY"))
                .andExpect(status().isOk());
        var auth = call("GET", up+"/authorities", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].uuid").exists()).andExpect(jsonPath("$.authorities").doesNotExist());
        assertFalse(body(auth).get("effectivePermissions").toString().contains(resource+":read"));
        call("DELETE", up+"/permissions/"+permission, admin, Map.of("reason", "Restore")).andExpect(status().isNoContent());
        assertTrue(body(call("GET", up+"/authorities", admin, null)).get("effectivePermissions").toString().contains(resource+":read"));
        call("DELETE", up+"/roles/"+role, admin, Map.of("reason", "Finished")).andExpect(status().isNoContent());
        call("DELETE", rp+"/permissions/"+permission, admin, Map.of("reason", "Finished")).andExpect(status().isNoContent());
        call("POST", rp+"/permissions", admin, Map.of("permissionUuids", List.of(permission))).andExpect(status().isOk());
        call("DELETE", rp, admin, null).andExpect(status().isNoContent());
        call("DELETE", pp, admin, null).andExpect(status().isNoContent());
    }
    @Test void paginationFilteringValidationAndProtectedRecordsAreEnforced() throws Exception {
        String admin = admin();
        call("GET", "/api/users?page=0&size=1&sort=username,asc", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0)).andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
        call("GET", "/api/roles?search=ADMIN&active=true", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("ADMIN"));
        call("GET", "/api/permissions?resource=user&action=read", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("user:read"));
        for (String query : List.of("size=0", "size=101", "page=-1", "sort=passwordHash,asc", "size=bad", "enabled=bad")) {
            call("GET", "/api/users?"+query, admin, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        }
        call("GET", "/api/users/not-a-uuid", admin, null).andExpect(status().isBadRequest());
        var role = use(istad.ha.iamservice.application.port.RoleRepository.class).findByName("ADMIN").orElseThrow();
        call("DELETE", "/api/roles/"+role.uuid(), admin, null).andExpect(status().isConflict());
        call("PATCH", "/api/me", token(user().uuid(), List.of()), "{\"phoneNumber\":\"invalid\"}").andExpect(status().isBadRequest());
    }
}
