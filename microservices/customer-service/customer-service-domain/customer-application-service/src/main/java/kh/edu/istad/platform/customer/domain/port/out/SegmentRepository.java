package kh.edu.istad.platform.customer.domain.port.out;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import kh.edu.istad.platform.customer.domain.dto.*;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface SegmentRepository {
    Mono<CustomerSegment> save(CustomerSegment segment);
    Mono<CustomerSegment> findById(UUID id);
    Flux<CustomerSegment> findAll(FilterRequest filter);
}
