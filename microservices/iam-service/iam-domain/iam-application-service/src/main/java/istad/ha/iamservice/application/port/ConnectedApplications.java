package istad.ha.iamservice.application.port;
import java.util.List;
import istad.ha.iamservice.application.result.ConnectedAppResult;
/** Authorization adapter state; callers supply the authenticated internal user's canonical username. */
public interface ConnectedApplications {
    List<ConnectedAppResult> list(String username);
    ConnectedAppResult describe(String clientId, java.util.Set<String> requestedScopes);
    void revoke(String username, String applicationId);
}
