package kh.edu.istad.platform.customer.persistence.config;
import kh.edu.istad.platform.customer.persistence.document.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Unique indexes enforce identity rules atomically, even when requests arrive concurrently. */
@Component
public class CustomerIndexes {
    private final Mono<Void> customers;
    private final Mono<Void> segments;
    public CustomerIndexes(ReactiveMongoTemplate template) {
        var active = PartialIndexFilter.of(Criteria.where("status").in("ACTIVE", "INACTIVE"));
        customers = Mono.defer(() -> template.indexOps(CustomerDocument.class)
                .createIndex(new Index().on("username", Sort.Direction.ASC).unique().partial(active)))
                .then(Mono.defer(() -> template.indexOps(CustomerDocument.class)
                        .createIndex(new Index().on("email", Sort.Direction.ASC).unique().partial(active))))
                .then(Mono.defer(() -> template.indexOps(CustomerDocument.class)
                        .createIndex(new Index().on("status", Sort.Direction.ASC).on("segmentId", Sort.Direction.ASC))))
                .then().cache();
        // Retired segment names remain reserved so historical reports keep an unambiguous identity.
        segments = Mono.defer(() -> template.indexOps(SegmentDocument.class)
                .createIndex(new Index().on("nameKey", Sort.Direction.ASC).unique())).then().cache();
    }
    public Mono<Void> customers() { return customers; }
    public Mono<Void> segments() { return segments; }
}
