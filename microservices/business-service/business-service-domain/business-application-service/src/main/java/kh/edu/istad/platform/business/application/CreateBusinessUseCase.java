package kh.edu.istad.platform.business.application;

import kh.edu.istad.platform.business.domain.Business;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@Component
public class CreateBusinessUseCase {
    private final BusinessRepository repository;
    public CreateBusinessUseCase(BusinessRepository repository) { this.repository = repository; }
    public Mono<Business> execute(CreateBusinessCommand command) {
        return Mono.defer(() -> repository.save(new Business(UUID.randomUUID(), command.name(),
                command.email(), command.address(), Business.Status.ACTIVE, Instant.now())));
    }
}
