package istad.ha.iamservice;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.domain.model.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.test.web.servlet.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Server-side browser contracts run with production precompiled templates and isolated test persistence. */
class BrowserIntegrationTests extends IamIntegrationTestSupport {
    static final String PASSWORD="Str0ng!Pass2026";
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext beans;
    <T> T use(Class<T> type) { return beans.getBean(type); }
    String unique() { return UUID.randomUUID().toString().replace("-", ""); }
    UserResult user() { String id=unique(); return use(RegisterUserUseCase.class).execute(new RegisterUserCommand("u"+id,id+"@example.com",PASSWORD,PASSWORD,"Dara","Sok","en")); }
    MockHttpSession login(UserResult user) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username",user.username()).param("password",PASSWORD))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }
    Map<String,String> profile(UserResult user) {
        return new HashMap<>(Map.of("email",user.email(),"givenName","Dara","familyName","Sok","locale","en","version",Long.toString(user.version())));
    }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postProfile(MockHttpSession session,Map<String,String> values) {
        var request=post("/profile/edit").session(session).with(csrf()); values.forEach(request::param); return request;
    }
    @Test void allAccountPagesRequireAnAuthenticatedInternalUser() throws Exception {
        for(String path:List.of("/","/profile/edit","/profile/password","/profile/accounts","/profile/apps")) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("unprovisioned-provider"))).andExpect(status().is4xxClientError());
        }
    }
    @Test void registrationIsCsrfProtectedKeepsOnlySafeFieldsAndCannotAssignRoles() throws Exception {
        String id=unique(); String name="u"+id;
        mvc.perform(post("/register").param("username",name)).andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("username","bad value").param("email","not-email")
                .param("givenName","<script>alert(1)</script>").param("password","secret-that-must-not-render").param("confirmPassword","mismatch"))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-that-must-not-render"))));
        mvc.perform(post("/register").with(csrf()).param("username",name).param("email",id+"@EXAMPLE.COM")
                .param("givenName","Dara").param("familyName","Sok").param("password",PASSWORD).param("confirmPassword",PASSWORD)
                .param("roles","ADMIN").param("emailVerified","true").param("locale","km")).andExpect(redirectedUrl("/login?registered"));
        var account=use(LoadUserForAuthenticationUseCase.class).execute(name);
        assertEquals(id+"@example.com",account.email()); assertFalse(account.emailVerified()); assertFalse(account.authorities().contains("ROLE_ADMIN"));
        mvc.perform(get("/login?registered")).andExpect(content().string(org.hamcrest.Matchers.containsString("Your account is ready.")));
    }
    @Test void languageCookiePersistsAndEveryBundleHasTheSameKeys() throws Exception {
        var response=mvc.perform(get("/login?lang=km")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"km\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ចូលគណនី"))).andReturn().getResponse();
        var cookie=response.getCookie("iam-language"); assertNotNull(cookie);
        mvc.perform(get("/register").cookie(cookie)).andExpect(content().string(org.hamcrest.Matchers.containsString("បង្កើតគណនី")));
        var english=new Properties(); var khmer=new Properties();
        try(var en=getClass().getResourceAsStream("/messages.properties");var km=getClass().getResourceAsStream("/messages_km.properties")) {
            english.load(new java.io.InputStreamReader(en,StandardCharsets.UTF_8));khmer.load(new java.io.InputStreamReader(km,StandardCharsets.UTF_8));
        }
        assertEquals(english.keySet(),khmer.keySet());
        for(String key:List.of("login.title","login.continueWith","register.title","consent.allow","consent.cancel","profile.title","profile.linkedAccounts","common.logout")) assertTrue(english.containsKey(key));
    }
    @Test void internalProfileShowsCurrentClaimsRolesAndAllPagesRenderInBothLanguages() throws Exception {
        var user=user(); var session=login(user);
        for(String path:List.of("/","/profile/edit","/profile/password","/profile/accounts","/profile/apps")) {
            mvc.perform(get(path).session(session).param("lang","en")).andExpect(status().isOk());
            mvc.perform(get(path).session(session).param("lang","km")).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"km\">")));
        }
        mvc.perform(get("/").session(session).param("lang","en")).andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(user.uuid().toString())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
    }
    @Test void profileEditsUseTheSessionOwnerResetVerificationAndRejectStaleVersions() throws Exception {
        var user=user();var other=user();var session=login(user);user=use(GetUserUseCase.class).execute(user.uuid());
        var input=profile(user);input.put("userUuid",other.uuid().toString());input.put("email",unique()+"@example.com"); input.put("phoneNumber","+85512345678");input.put("nickname","Dara nickname");
        mvc.perform(postProfile(session,input)).andExpect(redirectedUrl("/profile/edit?saved"));
        var updated=use(GetUserUseCase.class).execute(user.uuid());assertEquals(input.get("email"),updated.email()); assertFalse(updated.emailVerified());assertFalse(updated.profile().phoneNumberVerified());
        assertEquals(other.email(),use(GetUserUseCase.class).execute(other.uuid()).email());
        mvc.perform(postProfile(session,input)).andExpect(status().isConflict()).andExpect(content().string(org.hamcrest.Matchers.containsString("Reload the latest profile")));
        input.put("version",Long.toString(updated.version()));input.put("phoneNumber","not-a-phone");input.put("birthdate","2026-99-99");
        mvc.perform(postProfile(session,input)).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"not-a-phone\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-invalid=\"true\"")));
        assertEquals(updated.version(),use(GetUserUseCase.class).execute(user.uuid()).version());
        mvc.perform(post("/profile/edit").session(session)).andExpect(status().isForbidden());
    }
    @Test void passwordChangeRequiresCurrentPasswordAndNeverRendersSubmittedSecrets() throws Exception {
        var user=user();var session=login(user);
        mvc.perform(post("/profile/password").session(session).with(csrf()).param("currentPassword","wrong-secret")
                .param("newPassword","New!Password2026").param("confirmPassword","New!Password2026"))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString("The current password is incorrect.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("wrong-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("New!Password2026"))));
        mvc.perform(post("/profile/password").session(session).with(csrf()).param("currentPassword",PASSWORD)
                .param("newPassword","New!Password2026").param("confirmPassword","New!Password2026")).andExpect(redirectedUrl("/profile/password?saved"));
        mvc.perform(post("/login").with(csrf()).param("username",user.username()).param("password",PASSWORD)).andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).param("username",user.username()).param("password","New!Password2026")).andExpect(redirectedUrl("/"));
        mvc.perform(post("/profile/password").session(session)).andExpect(status().isForbidden());
    }
    @Test void unlinkConfirmationIsReadOnlyAndOnlyTheOwnerCanDisconnect() throws Exception {
        var user=user();var other=user();
        var identity=use(LinkIdentityUseCase.class).execute(user.uuid(),new FederatedIdentityCommand(Provider.GITHUB,unique(),null,false,"Dara",null,null,Map.of(),null,null,"en"));
        var session=login(user);
        mvc.perform(get("/profile/accounts/"+identity.uuid()+"/unlink").session(session)).andExpect(status().isOk());
        assertEquals(1,use(ListUserIdentitiesUseCase.class).execute(user.uuid()).size());
        mvc.perform(get("/profile/accounts/"+identity.uuid()+"/unlink").session(login(other))).andExpect(status().isNotFound());
        mvc.perform(post("/profile/accounts/"+identity.uuid()+"/unlink").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/profile/accounts/"+identity.uuid()+"/unlink").session(session).with(csrf())).andExpect(redirectedUrl("/profile/accounts?unlinked"));
        assertTrue(use(ListUserIdentitiesUseCase.class).execute(user.uuid()).isEmpty());
    }
    RegisteredClient client(boolean consent) {
        var client=RegisteredClient.withId(unique()).clientId("browser_"+unique()).clientName("Browser <script> app")
                .clientSecret(use(org.springframework.security.crypto.password.PasswordEncoder.class).encode("browser-client-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN).redirectUri("http://localhost/browser-callback").scope("openid").scope("profile").scope("email")
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(consent).build()).build();
        use(RegisteredClientRepository.class).save(client); return client;
    }
    String consent(MockHttpSession session,RegisteredClient client) throws Exception {
        String verifier=unique()+unique(); String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        return mvc.perform(get("/oauth2/authorize").session(session).queryParam("response_type","code").queryParam("client_id",client.getClientId())
                .queryParam("redirect_uri","http://localhost/browser-callback").queryParam("scope","openid profile email").queryParam("state","caller-state")
                .queryParam("code_challenge",challenge).queryParam("code_challenge_method","S256"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }
    String state(String location) {
        return Arrays.stream(URI.create(location).getRawQuery().split("&")).filter(p -> p.startsWith("state=")).map(p -> URLDecoder.decode(p.substring(6),StandardCharsets.UTF_8)).findFirst().orElseThrow();
    }
    @Test void consentPageEscapesClientMetadataPreservesLanguageContextAndAllowsOnlySelectedScopes() throws Exception {
        var user=user();var session=login(user);var client=client(true);String location=consent(session,client);
        mvc.perform(get(URI.create(location)).session(session)).andExpect(status().isOk()).andExpect(view().name("consent"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Browser &lt;script&gt; app")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("lang=km")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Browser <script> app"))));
        mvc.perform(post("/oauth2/authorize").session(session).param("client_id",client.getClientId()).param("state",state(location)).param("scope","email"))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location",org.hamcrest.Matchers.containsString("code=")));
        var consent=use(OAuth2AuthorizationConsentService.class).findById(client.getId(),user.username());assertNotNull(consent);assertTrue(consent.getScopes().contains("email"));assertFalse(consent.getScopes().contains("profile"));
    }
    @Test void cancellingConsentDoesNotCreateConsentOrIssueACode() throws Exception {
        var user=user();var session=login(user);var client=client(true);String location=consent(session,client);
        mvc.perform(post("/oauth2/authorize").session(session).param("client_id",client.getClientId()).param("state",state(location)))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location",org.hamcrest.Matchers.containsString("error=access_denied")));
        assertNull(use(OAuth2AuthorizationConsentService.class).findById(client.getId(),user.username()));
    }
    OAuth2Authorization grant(UserResult user,RegisteredClient client) {
        Instant now=Instant.now();
        var authorization=OAuth2Authorization.withRegisteredClient(client).principalName(user.username()).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid","email")).accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,"access-"+unique(),now,now.plusSeconds(300),Set.of("openid","email")))
                .refreshToken(new OAuth2RefreshToken("refresh-"+unique(),now,now.plusSeconds(600))).build();
        use(OAuth2AuthorizationService.class).save(authorization);
        use(OAuth2AuthorizationConsentService.class).save(OAuth2AuthorizationConsent.withId(client.getId(),user.username()).scope("email").build()); return authorization;
    }
    @Test void appRevocationRemovesOnlyTheOwnersAuthorizationAndConsentAndPreventsRefresh() throws Exception {
        var user=user();var other=user();var client=client(true);var another=client(true);
        var own=grant(user,client);var foreign=grant(other,client);var kept=grant(user,another);var session=login(user);
        mvc.perform(get("/profile/apps").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Browser &lt;script&gt; app")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(own.getRefreshToken().getToken().getTokenValue()))));
        mvc.perform(get("/profile/apps/"+client.getId()+"/revoke").session(session)).andExpect(status().isOk());
        assertNotNull(use(OAuth2AuthorizationService.class).findById(own.getId()));
        mvc.perform(post("/profile/apps/"+client.getId()+"/revoke").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/profile/apps/"+client.getId()+"/revoke").session(session).with(csrf())).andExpect(redirectedUrl("/profile/apps?revoked"));
        assertNull(use(OAuth2AuthorizationService.class).findById(own.getId()));assertNull(use(OAuth2AuthorizationConsentService.class).findById(client.getId(),user.username()));
        assertNotNull(use(OAuth2AuthorizationService.class).findById(foreign.getId()));assertNotNull(use(OAuth2AuthorizationService.class).findById(kept.getId()));
        mvc.perform(post("/oauth2/token").with(httpBasic(client.getClientId(),"browser-client-secret")).param("grant_type","refresh_token")
                .param("refresh_token",own.getRefreshToken().getToken().getTokenValue())).andExpect(status().isBadRequest());
    }
    @Test void appConfirmationAndRevocationCannotTargetAnotherUsersGrant() throws Exception {
        var owner=user();var attacker=user();var client=client(true);var authorization=grant(owner,client);var session=login(attacker);
        mvc.perform(get("/profile/apps/"+client.getId()+"/revoke").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/profile/apps/"+client.getId()+"/revoke").session(session).with(csrf())).andExpect(status().isBadRequest());
        assertNotNull(use(OAuth2AuthorizationService.class).findById(authorization.getId()));
    }
    @Test void theLastSocialSignInMethodCannotBeDisconnectedThroughTheBrowser() throws Exception {
        String id=unique();
        var account=use(FindOrProvisionFederatedUserUseCase.class).execute(new FederatedIdentityCommand(Provider.GITHUB,id,id+"@example.com",false,"Dara",null,null,Map.of(),"Dara","Sok","en"));
        var principal=new istad.ha.iamservice.infrastructure.security.IamUserPrincipal(account.uuid(),account.username(),account.authorities(),true,true,true,true);
        var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
        var session=new MockHttpSession();
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new org.springframework.security.core.context.SecurityContextImpl(auth));
        var identity=use(ListUserIdentitiesUseCase.class).execute(account.uuid()).get(0);
        var result=mvc.perform(post("/profile/accounts/"+identity.uuid()+"/unlink").session(session).with(csrf()))
                .andExpect(redirectedUrl("/profile/accounts?error")).andReturn();
        mvc.perform(get("/profile/accounts?error").session(session).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Keep at least one sign-in method.")));
        assertEquals(1,use(ListUserIdentitiesUseCase.class).execute(account.uuid()).size());
    }

}
