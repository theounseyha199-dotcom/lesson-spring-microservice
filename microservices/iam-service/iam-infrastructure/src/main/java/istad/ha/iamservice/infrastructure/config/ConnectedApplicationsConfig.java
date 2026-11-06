package istad.ha.iamservice.infrastructure.config;
import istad.ha.iamservice.application.port.ConnectedApplications;
import istad.ha.iamservice.application.usecase.*;
import org.springframework.context.annotation.*;
@Configuration
public class ConnectedApplicationsConfig {
    @Bean public GetConsentClientUseCase consentClient(ConnectedApplications applications) { return new GetConsentClientUseCase(applications); }
    @Bean public ListConnectedAppsUseCase listConnectedApps(ConnectedApplications applications) { return new ListConnectedAppsUseCase(applications); }
    @Bean public RevokeConnectedAppUseCase revokeConnectedApp(ConnectedApplications applications) { return new RevokeConnectedAppUseCase(applications); }
}
