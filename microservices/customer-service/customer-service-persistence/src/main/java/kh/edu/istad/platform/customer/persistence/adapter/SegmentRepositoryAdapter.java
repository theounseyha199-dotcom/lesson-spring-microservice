package kh.edu.istad.platform.customer.persistence.adapter;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.port.out.SegmentRepository;
import kh.edu.istad.platform.customer.persistence.config.CustomerIndexes;
import kh.edu.istad.platform.customer.persistence.document.SegmentDocument;
import kh.edu.istad.platform.customer.persistence.repository.SegmentMongoRepository;
import kh.edu.istad.platform.customer.persistence.query.DynamicMongoQuery;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.*;
import java.util.*;
import static kh.edu.istad.platform.customer.persistence.query.DynamicMongoQuery.*;

/** Maps segment aggregates to MongoDB and translates uniqueness and concurrent-edit conflicts. */
@Repository
public class SegmentRepositoryAdapter implements SegmentRepository {
    private final SegmentMongoRepository repository;
    private final ReactiveMongoTemplate template;
    private final CustomerIndexes indexes;
    private static final Map<String, Field> FIELDS = Map.of("segmentId", uuid("_id"), "name", text("name"),
            "description", text("description"), "status", enumeration("status", CustomerSegment.Status.class),
            "createdAt", instant("createdAt"), "updatedAt", instant("updatedAt"));
    public SegmentRepositoryAdapter(SegmentMongoRepository repository, ReactiveMongoTemplate template, CustomerIndexes indexes) {
        this.repository = repository; this.template = template; this.indexes = indexes;
    }
    public Mono<CustomerSegment> save(CustomerSegment s) {
        return indexes.segments().then(Mono.defer(() -> repository.save(new SegmentDocument(s.segmentId().toString(),
                s.name(), s.nameKey(), s.description(), s.status(), s.createdAt(), s.updatedAt(), s.version())))).map(this::domain)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        error -> new kh.edu.istad.platform.customer.domain.exception.ConflictException("Segment name already exists"))
                .onErrorMap(org.springframework.dao.OptimisticLockingFailureException.class,
                        error -> new kh.edu.istad.platform.customer.domain.exception.ConflictException("Segment changed; reload and retry"));
    }
    public Mono<CustomerSegment> findById(UUID id) {
        return repository.findById(id.toString()).filter(s -> s.status() != CustomerSegment.Status.DELETED).map(this::domain);
    }
    public Flux<CustomerSegment> findAll(FilterRequest filter) {
        return Flux.defer(() -> template.find(DynamicMongoQuery.page(filter, FIELDS, List.of("name", "description")),
                SegmentDocument.class).map(this::domain));
    }
    private CustomerSegment domain(SegmentDocument s) {
        return new CustomerSegment(UUID.fromString(s.id()), s.name(), s.description(), s.status(),
                s.createdAt(), s.updatedAt(), s.version());
    }
}
