package istad.ha.iamservice.application.usecase;
import istad.ha.iamservice.application.port.ConnectedApplications;
public final class RevokeConnectedAppUseCase {
    private final ConnectedApplications applications;
    public RevokeConnectedAppUseCase(ConnectedApplications applications) { this.applications = applications; }
    public void execute(String username, String applicationId) { applications.revoke(username, applicationId); }
}
