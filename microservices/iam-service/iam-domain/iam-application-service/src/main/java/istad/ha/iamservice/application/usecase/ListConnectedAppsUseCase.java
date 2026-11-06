package istad.ha.iamservice.application.usecase;
import istad.ha.iamservice.application.port.ConnectedApplications;
public final class ListConnectedAppsUseCase {
    private final ConnectedApplications applications;
    public ListConnectedAppsUseCase(ConnectedApplications applications) { this.applications = applications; }
    public java.util.List<istad.ha.iamservice.application.result.ConnectedAppResult> execute(String username) { return applications.list(username); }
}
