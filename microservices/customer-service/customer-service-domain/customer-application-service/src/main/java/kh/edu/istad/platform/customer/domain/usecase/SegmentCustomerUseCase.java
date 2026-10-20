package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.exception.*;
import kh.edu.istad.platform.customer.domain.port.out.SegmentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import java.time.Instant;
import java.util.UUID;

/** Coordinates segment CRUD and checks eligibility before a customer membership is saved. */
@Service
public class SegmentCustomerUseCase {
    private final SegmentRepository repository;
    public SegmentCustomerUseCase(SegmentRepository repository) { this.repository = repository; }
    public Mono<CustomerSegment> create(String name, String description) {
        return Mono.defer(() -> {
            var now = Instant.now();
            return repository.save(new CustomerSegment(UUID.randomUUID(), name, description,
                    CustomerSegment.Status.ACTIVE, now, now, null));
        });
    }
    public Mono<CustomerSegment> get(UUID id) {
        return repository.findById(id).switchIfEmpty(Mono.error(new ResourceNotFoundException("Segment")));
    }
    public Flux<CustomerSegment> list(FilterRequest filter) { return repository.findAll(filter); }
    public Mono<CustomerSegment> update(UUID id, String name, String description, CustomerSegment.Status status) {
        return get(id).flatMap(existing -> repository.save(existing.update(name, description, status)));
    }
    public Mono<Void> delete(UUID id) { return get(id).flatMap(s -> repository.save(s.deleted())).then(); }

    // Only active segments accept new memberships. Existing memberships survive segment retirement.
    public Mono<Void> requireAssignable(UUID id) {
        if (id == null) return Mono.empty();
        return get(id).flatMap(segment -> segment.status() == CustomerSegment.Status.ACTIVE
                ? Mono.empty() : Mono.error(new ConflictException("Only active segments accept customers")));
    }
}
