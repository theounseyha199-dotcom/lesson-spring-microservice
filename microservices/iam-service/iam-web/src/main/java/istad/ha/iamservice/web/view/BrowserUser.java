package istad.ha.iamservice.web.view;
import istad.ha.iamservice.application.port.CurrentActorProvider;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.application.usecase.GetUserUseCase;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class BrowserUser {
    private final CurrentActorProvider actors; private final GetUserUseCase users;
    public BrowserUser(CurrentActorProvider actors, GetUserUseCase users) { this.actors = actors; this.users = users; }
    public UserResult current() {
        UUID id;
        try { id = UUID.fromString(actors.currentActor()); }
        catch (IllegalArgumentException ex) { throw new AccessDeniedException("Internal browser user required"); }
        var user = users.execute(id);
        if (!user.enabled() || !user.accountNonLocked() || !user.accountNonExpired() || !user.credentialsNonExpired())
            throw new AccessDeniedException("Account unavailable");
        return user;
    }
}
