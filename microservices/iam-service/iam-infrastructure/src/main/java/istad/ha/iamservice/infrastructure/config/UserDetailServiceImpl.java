package istad.ha.iamservice.infrastructure.config;

import istad.ha.iamservice.application.usecase.LoadUserForAuthenticationUseCase;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
    private final LoadUserForAuthenticationUseCase load;
    public UserDetailServiceImpl(LoadUserForAuthenticationUseCase load) { this.load = load; }
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try { return IamUserPrincipal.from(load.execute(username)); }
        catch (ApplicationException ex) {
            if ("not_found".equals(ex.code())) throw new UsernameNotFoundException("User not found");
            throw ex;
        }
    }
}
