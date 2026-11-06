package istad.ha.iamservice.application.result;
import java.util.Set;
public record ConnectedAppResult(String id, String name, Set<String> scopes) {
    public ConnectedAppResult { scopes = Set.copyOf(scopes); }
}
