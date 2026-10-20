package kh.edu.istad.platform.customer.persistence.adapter;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.persistence.config.CustomerIndexes;
import kh.edu.istad.platform.customer.persistence.mapper.CustomerPersistenceMapper;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import kh.edu.istad.platform.customer.persistence.query.*;
import org.bson.Document;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.*;
import java.time.Instant;
import java.util.*;
import static kh.edu.istad.platform.customer.persistence.query.CustomerQueries.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/** Nonblocking persistence with conditional updates, unique identities, audit preservation, and aggregation. */
@Repository
public class CustomerReactiveRepositoryAdapter implements CustomerRepository {
    private final ReactiveMongoTemplate template;
    private final CustomerPersistenceMapper mapper;
    private final CustomerIndexes indexes;
    private final ReactiveAuditorAware<String> auditor;
    public CustomerReactiveRepositoryAdapter(ReactiveMongoTemplate template, CustomerPersistenceMapper mapper,
                                             CustomerIndexes indexes, ReactiveAuditorAware<String> auditor) {
        this.template = template; this.mapper = mapper; this.indexes = indexes; this.auditor = auditor;
    }
    public Mono<Customer> save(Customer customer) {
        return indexes.customers().then(Mono.defer(() -> {
            CustomerDocument document = mapper.toDocument(customer);
            return template.findById(document.customerId(), CustomerDocument.class).flatMap(existing -> {
                if (existing.status() == kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus.DELETED)
                    return Mono.<CustomerDocument>error(new CustomerNotFoundException(customer.getId().value()));
                return auditor.getCurrentAuditor().defaultIfEmpty("SYSTEM").flatMap(user -> {
                    // An atomic conditional update prevents a concurrent edit from resurrecting a deleted customer.
                    Query query = Query.query(Criteria.where("_id").is(document.customerId()).and("status").ne("DELETED")
                            .and("version").is(customer.getVersion()));
                    Update update = new Update().set("username", document.username()).set("familyName", document.familyName())
                            .set("givenName", document.givenName()).set("email", document.email()).set("phoneNumber", document.phoneNumber())
                            .set("status", document.status()).set("segmentId", document.segmentId())
                            .set("updatedAt", Instant.now()).set("updatedBy", user).inc("version", 1);
                    return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), CustomerDocument.class)
                            .switchIfEmpty(Mono.error(new org.springframework.dao.OptimisticLockingFailureException("Customer changed; reload and retry")));
                });
            }).switchIfEmpty(Mono.defer(() -> template.insert(document))).map(mapper::toDomain);
        })).onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                error -> new kh.edu.istad.platform.customer.domain.exception.ConflictException("Username or email already exists"))
                .onErrorMap(org.springframework.dao.OptimisticLockingFailureException.class,
                        error -> new kh.edu.istad.platform.customer.domain.exception.ConflictException("Customer changed; reload and retry"));
    }
    public Mono<Customer> findById(CustomerId id) {
        return template.findOne(Query.query(Criteria.where("_id").is(id.value().toString()).and("status").ne("DELETED")),
                CustomerDocument.class).map(mapper::toDomain);
    }
    public Flux<Customer> findAll() { return findAll(new FilterRequest(List.of(), null, 0, 20, "createdAt", "desc")); }
    public Flux<Customer> findAll(FilterRequest filter) {
        return Flux.defer(() -> template.find(DynamicMongoQuery.page(filter, FIELDS, SEARCH), CustomerDocument.class).map(mapper::toDomain));
    }
    public Mono<Boolean> deleteById(CustomerId id) {
        return auditor.getCurrentAuditor().defaultIfEmpty("SYSTEM").flatMap(user -> {
            Instant now = Instant.now();
            return template.updateFirst(Query.query(Criteria.where("_id").is(id.value().toString()).and("status").ne("DELETED")),
                    new Update().set("status", "DELETED").set("deletedAt", now).set("deletedBy", user)
                            .set("updatedAt", now).set("updatedBy", user).inc("version", 1), CustomerDocument.class);
        }).map(result -> result.getModifiedCount() > 0);
    }
    public Mono<CustomerStatistics> statistics(FilterRequest filter) {
        return Mono.defer(() -> {
            var pipeline = newAggregation(match(DynamicMongoQuery.criteria(filter, FIELDS, SEARCH)),
                    facet(count().as("total")).as("totals")
                            .and(group("status").count().as("total"), sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")).as("statuses")
                            .and(group("segmentId").count().as("total"), sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")).as("segments")
                            .and(group("status", "segmentId").count().as("total"), sort(org.springframework.data.domain.Sort.Direction.ASC, "_id.status", "_id.segmentId")).as("combined"));
            return template.aggregate(pipeline, "customers", Document.class).single().map(this::statistics);
        });
    }
    private CustomerStatistics statistics(Document result) {
        var totals = result.getList("totals", Document.class);
        return new CustomerStatistics(totals.isEmpty() ? 0 : number(totals.getFirst()),
                result.getList("statuses", Document.class).stream()
                        .map(d -> new CustomerStatistics.StatusCount(d.getString("_id"), number(d))).toList(),
                result.getList("segments", Document.class).stream()
                        .map(d -> new CustomerStatistics.SegmentCount(d.getString("_id"), number(d))).toList(),
                result.getList("combined", Document.class).stream().map(d -> {
                    Document key = d.get("_id", Document.class);
                    return new CustomerStatistics.StatusSegmentCount(key.getString("status"), key.getString("segmentId"), number(d));
                }).toList());
    }
    private long number(Document document) { return ((Number) document.get("total")).longValue(); }
}
