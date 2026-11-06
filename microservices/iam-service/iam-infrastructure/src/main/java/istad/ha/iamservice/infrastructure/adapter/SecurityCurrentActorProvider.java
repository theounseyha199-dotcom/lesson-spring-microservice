package istad.ha.iamservice.infrastructure.adapter;

import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public final class SecurityCurrentActorProvider implements CurrentActorProvider {
    private final UserRepository users;
    public SecurityCurrentActorProvider(UserRepository users) { this.users = users; }
    @Override public String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) return "anonymous";
        if (authentication.getPrincipal() instanceof IamUserPrincipal user) return user.uuid().toString();
        if (authentication.getPrincipal() instanceof Jwt jwt) return jwt.getSubject();
        if (authentication.getPrincipal() instanceof OAuth2User user) {
            Object uuid = user.getAttribute("iam_user_uuid"); return uuid == null ? "external" : uuid.toString();
        }
        return users.findByUsernameOrEmail(authentication.getName()).map(user -> user.uuid().toString()).orElse(authentication.getName());
    }
}
