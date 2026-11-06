package istad.ha.iamservice.infrastructure.federation;

import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;

/** Link intent is bound to the server-stored OAuth request/state, not callback parameters. */
public final class FederationAuthorizationRequests implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OWNER = "iam_link_owner";
    public static final String REQUEST = FederationAuthorizationRequests.class.getName() + ".request";
    public static final String ORIGINAL = FederationAuthorizationRequests.class.getName() + ".original";
    private final HttpSessionOAuth2AuthorizationRequestRepository delegate = new HttpSessionOAuth2AuthorizationRequestRepository();
    @Override public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }
    @Override public void saveAuthorizationRequest(OAuth2AuthorizationRequest value, HttpServletRequest request, HttpServletResponse response) {
        delegate.saveAuthorizationRequest(value, request, response);
    }
    @Override public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        var value = delegate.removeAuthorizationRequest(request, response);
        if (value != null) {
            request.setAttribute(REQUEST, value);
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            String owner = value.getAttribute(OWNER);
            if (owner != null && current != null && current.isAuthenticated()
                    && current.getPrincipal() instanceof IamUserPrincipal user && owner.equals(user.uuid().toString())) {
                request.setAttribute(ORIGINAL, current);
            }
        }
        return value;
    }
}
