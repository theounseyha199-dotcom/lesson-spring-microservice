package istad.ha.iamservice.infrastructure.federation;

import istad.ha.iamservice.application.AuthUserResult;
import istad.ha.iamservice.application.command.FederatedIdentityCommand;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public final class FederationLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FederationLoginHandler.class);
    public static final String PENDING = FederationLoginHandler.class.getName() + ".pending";
    public record Pending(FederatedIdentityCommand identity, Instant expiresAt, String flow) {
        @Override public String toString() { return "Pending[identity=redacted]"; }
    }
    private final ProviderIdentityMapper mapper;
    private final FindOrProvisionFederatedUserUseCase provision;
    private final LinkIdentityUseCase link;
    private final GetUserUseCase users;
    private final SavedRequestAwareAuthenticationSuccessHandler success = new SavedRequestAwareAuthenticationSuccessHandler();
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    public FederationLoginHandler(ProviderIdentityMapper mapper, FindOrProvisionFederatedUserUseCase provision,
            LinkIdentityUseCase link, GetUserUseCase users) {
        this.mapper = mapper; this.provision = provision; this.link = link; this.users = users;
    }
    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        request.getSession().removeAttribute(PENDING);
        var authorization = (OAuth2AuthorizationRequest) request.getAttribute(FederationAuthorizationRequests.REQUEST);
        boolean linking = authorization != null && authorization.getAttribute(FederationAuthorizationRequests.OWNER) != null;
        try {
            var oauth = (OAuth2AuthenticationToken) authentication;
            var command = mapper.map(oauth.getAuthorizedClientRegistrationId(), oauth.getPrincipal());
            if (linking) {
                Authentication original = original(request);
                if (original == null) throw new ApplicationException("link_session_changed", "Sign in again before linking");
                var owner = (IamUserPrincipal) original.getPrincipal();
                var user = users.execute(owner.uuid());
                if (!user.enabled() || !user.accountNonLocked() || !user.accountNonExpired() || !user.credentialsNonExpired())
                    throw new ApplicationException("account_unavailable", "Account cannot sign in");
                // Restore internal owner before auditing the use case; never audit as the external subject.
                save(request, response, original);
                link.execute(owner.uuid(), command);
                response.sendRedirect(request.getContextPath() + "/profile/accounts?linked");
            } else {
                // The temporary OAuth principal must never become an IAM browser session.
                save(request, response, null);
                try { authenticate(request, response, provision.execute(command)); }
                catch (ApplicationException ex) {
                    if (!"profile_completion_required".equals(ex.code())) throw ex;
                    request.getSession().setAttribute(PENDING, new Pending(command, Instant.now().plusSeconds(300), java.util.UUID.randomUUID().toString()));
                    response.sendRedirect(request.getContextPath() + "/federation/complete");
                }
            }
        } catch (RuntimeException ex) {
            LOG.warn("Federation callback rejected ({})", ex.getClass().getSimpleName());
            save(request, response, linking ? original(request) : null);
            response.sendRedirect(request.getContextPath() + (linking && original(request) != null
                    ? "/profile/accounts?error" : "/login?oauth2error"));
        }
    }
    public void authenticate(HttpServletRequest request, HttpServletResponse response, AuthUserResult result)
            throws IOException, ServletException {
        if (!result.enabled() || !result.accountNonLocked() || !result.accountNonExpired() || !result.credentialsNonExpired())
            throw new ApplicationException("account_unavailable", "Account cannot sign in");
        var principal = new IamUserPrincipal(result.uuid(), result.username(), result.authorities(), result.accountNonExpired(),
                result.accountNonLocked(), result.credentialsNonExpired(), result.enabled());
        var internal = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        save(request, response, internal);
        success.onAuthenticationSuccess(request, response, internal);
    }
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        var session = request.getSession(false);
        if (session != null) session.removeAttribute(PENDING);
        Authentication original = original(request);
        save(request, response, original);
        response.sendRedirect(request.getContextPath() + (original == null ? "/login?oauth2error" : "/profile/accounts?error"));
    }
    private Authentication original(HttpServletRequest request) {
        return (Authentication) request.getAttribute(FederationAuthorizationRequests.ORIGINAL);
    }
    private void save(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
    }
}
