package istad.ha.iamservice.infrastructure.security;

import istad.ha.iamservice.application.usecase.RecordLoginAttemptUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class LoginAttemptListener {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LoginAttemptListener.class);
    private final RecordLoginAttemptUseCase attempts;
    public LoginAttemptListener(RecordLoginAttemptUseCase attempts) { this.attempts = attempts; }
    @EventListener public void success(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof IamUserPrincipal principal) record(principal.getUsername(), true);
    }
    @EventListener public void failure(AbstractAuthenticationFailureEvent event) {
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) record(event.getAuthentication().getName(), false);
    }
    private void record(String username, boolean success) {
        try { attempts.execute(username, success); }
        catch (RuntimeException ex) { LOG.warn("Unable to persist a login attempt ({})", ex.getClass().getSimpleName()); }
    }
}
