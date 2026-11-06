package istad.ha.iamservice.infrastructure.adapter;

import istad.ha.iamservice.application.AuthUserResult;
import istad.ha.iamservice.application.port.UserAuthenticationPort;
import istad.ha.iamservice.application.usecase.LoadUserForAuthenticationUseCase;
import istad.ha.iamservice.application.exception.ApplicationException;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public final class UserAuthenticationAdapter implements UserAuthenticationPort {
    private final LoadUserForAuthenticationUseCase load;
    public UserAuthenticationAdapter(LoadUserForAuthenticationUseCase load) { this.load = load; }
    @Override public Optional<AuthUserResult> findByUsername(String value) {
        try { return Optional.of(load.execute(value)); }
        catch (ApplicationException ex) { if ("not_found".equals(ex.code())) return Optional.empty(); throw ex; }
    }
}
