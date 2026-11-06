package istad.ha.iamservice.application.port;

import istad.ha.iamservice.application.AuthUserResult;
import java.util.Optional;

public interface UserAuthenticationPort {
    Optional<AuthUserResult> findByUsername(String username);
}
