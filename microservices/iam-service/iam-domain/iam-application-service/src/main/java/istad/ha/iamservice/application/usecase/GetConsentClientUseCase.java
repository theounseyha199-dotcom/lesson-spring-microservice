package istad.ha.iamservice.application.usecase;
import istad.ha.iamservice.application.port.ConnectedApplications;
import istad.ha.iamservice.application.result.ConnectedAppResult;
import java.util.Set;
public final class GetConsentClientUseCase {
    private final ConnectedApplications applications;
    public GetConsentClientUseCase(ConnectedApplications applications) { this.applications = applications; }
    public ConnectedAppResult execute(String clientId, Set<String> scopes) { return applications.describe(clientId, scopes); }
}
