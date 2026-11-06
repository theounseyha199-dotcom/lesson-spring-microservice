package istad.ha.iamservice.infrastructure.federation;

import istad.ha.iamservice.application.command.FederatedIdentityCommand;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Browser-only federation endpoints; all state-changing requests retain Spring CSRF checks. */
@Controller
public final class FederationController {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FederationController.class);
    private final FindOrProvisionFederatedUserUseCase provision;
    private final ListUserIdentitiesUseCase identities;
    private final FederationLoginHandler handler;
    private final OAuth2AuthorizationRequestResolver resolver;
    private final FederationAuthorizationRequests requests;
    public FederationController(FindOrProvisionFederatedUserUseCase provision, ListUserIdentitiesUseCase identities,
            FederationLoginHandler handler, OAuth2AuthorizationRequestResolver resolver, FederationAuthorizationRequests requests) {
        this.provision = provision; this.identities = identities; this.handler = handler; this.resolver = resolver; this.requests = requests;
    }
    @GetMapping("/profile/accounts")
    public String accounts(Authentication authentication, HttpServletRequest request, Model model) {
        var owner = owner(authentication);
        common(request, model);
        model.addAttribute("identities", identities.execute(owner.uuid()));
        model.addAttribute("linked", request.getParameterMap().containsKey("linked"));
        model.addAttribute("failed", request.getParameterMap().containsKey("error"));
        model.addAttribute("unlinked", request.getParameterMap().containsKey("unlinked"));
        return "linked-accounts";
    }
    @PostMapping("/profile/accounts/link/{provider}")
    public void link(@PathVariable("provider") String provider, Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        var owner = owner(authentication);
        if (!Set.of("google", "github", "facebook", "telegram").contains(provider)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        request.getSession().removeAttribute(FederationLoginHandler.PENDING);
        var authorization = OAuth2AuthorizationRequest.from(resolver.resolve(request, provider))
                .attributes(attributes -> attributes.put(FederationAuthorizationRequests.OWNER, owner.uuid().toString())).build();
        requests.saveAuthorizationRequest(authorization, request, response);
        response.sendRedirect(authorization.getAuthorizationRequestUri());
    }
    @GetMapping("/federation/complete")
    public String complete(HttpServletRequest request, Model model) {
        var pending = pending(request);
        if (pending == null) return "redirect:/login?oauth2error";
        return form(request, model, pending, "", Map.of());
    }
    @PostMapping("/federation/complete")
    public String complete(@RequestParam("flow") String flow, @RequestParam(name="email", defaultValue="") String email,
            @RequestParam(name="givenName", defaultValue="") String givenName, @RequestParam(name="familyName", defaultValue="") String familyName,
            HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, ServletException {
        var session = request.getSession(false);
        if (session == null) return "redirect:/login?oauth2error";
        synchronized (session) {
            var pending = pending(request);
            if (pending == null || !pending.flow().equals(flow)) return "redirect:/login?oauth2error";
            var original = pending.identity();
            var values = Map.of("email", email, "givenName", givenName, "familyName", familyName);
            var errors = new LinkedHashMap<String, String>();
            if (original.email() == null) {
                try { istad.ha.iamservice.domain.rule.Checks.email(email); }
                catch (istad.ha.iamservice.domain.exception.DomainException ex) { errors.put("email", "validation.email"); }
            }
            if (original.givenName() == null && (givenName.isBlank() || givenName.strip().length() > 100))
                errors.put("givenName", "validation.name");
            if (original.familyName() == null && (familyName.isBlank() || familyName.strip().length() > 100))
                errors.put("familyName", "validation.name");
            if (!errors.isEmpty()) {
                response.setStatus(400); model.addAttribute("fieldErrors", errors);
                return form(request, model, pending, "validation", values);
            }
            try {
                var command = new FederatedIdentityCommand(original.provider(), original.providerSubject(),
                        original.email() == null ? blank(email) : original.email(), original.emailVerified(),
                        original.displayName(), original.username(), original.pictureUrl(), original.attributes(),
                        original.givenName() == null ? blank(givenName) : original.givenName(),
                        original.familyName() == null ? blank(familyName) : original.familyName(), original.locale());
                var user = provision.execute(command);
                session.removeAttribute(FederationLoginHandler.PENDING);
                request.changeSessionId();
                handler.authenticate(request, response, user);
                return null;
            } catch (RuntimeException ex) {
                LOG.warn("Federated profile completion failed ({})", ex.getClass().getSimpleName());
                // Do not expose provider data, uniqueness details or exception text to the browser.
                String error = ex instanceof ApplicationException app && "identity_link_required".equals(app.code())
                        ? "identity_link_required"
                        : "validation";
                response.setStatus(400);
                return form(request, model, pending, error, Map.of("email", email, "givenName", givenName, "familyName", familyName));
            }
        }
    }
    @PostMapping("/federation/cancel")
    public String cancel(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.removeAttribute(FederationLoginHandler.PENDING);
        return "redirect:/login";
    }
    private FederationLoginHandler.Pending pending(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return null;
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof IamUserPrincipal) {
            session.removeAttribute(FederationLoginHandler.PENDING); return null;
        }
        var value = (FederationLoginHandler.Pending) session.getAttribute(FederationLoginHandler.PENDING);
        if (value == null || !value.expiresAt().isAfter(Instant.now())) {
            session.removeAttribute(FederationLoginHandler.PENDING); return null;
        }
        return value;
    }
    private String form(HttpServletRequest request, Model model, FederationLoginHandler.Pending pending,
            String error, Map<String, String> values) {
        common(request, model);
        if (!model.containsAttribute("fieldErrors")) model.addAttribute("fieldErrors", Map.of());
        model.addAttribute("flow", pending.flow());
        model.addAttribute("provider", pending.identity().provider().name());
        model.addAttribute("error", error);
        var identity = pending.identity();
        model.addAttribute("email", identity.email() == null ? values.getOrDefault("email", "") : identity.email());
        model.addAttribute("givenName", identity.givenName() == null ? values.getOrDefault("givenName", "") : identity.givenName());
        model.addAttribute("familyName", identity.familyName() == null ? values.getOrDefault("familyName", "") : identity.familyName());
        model.addAttribute("needEmail", identity.email() == null);
        model.addAttribute("needGivenName", identity.givenName() == null);
        model.addAttribute("needFamilyName", identity.familyName() == null);
        return "federation-complete";
    }
    private void common(HttpServletRequest request, Model model) {
        var csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("csrfParameterName", csrf.getParameterName()); model.addAttribute("csrfToken", csrf.getToken());
    }
    private IamUserPrincipal owner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof IamUserPrincipal user))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user;
    }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
