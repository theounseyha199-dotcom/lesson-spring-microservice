package kh.edu.istad.platform.customer.persistence.adapter;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.exception.*;
import kh.edu.istad.platform.customer.persistence.config.CustomerIndexes;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import kh.edu.istad.platform.customer.persistence.mapper.CustomerPersistenceMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.bson.Document;
import reactor.core.publisher.*;
import reactor.test.StepVerifier;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerReactiveRepositoryAdapterTest {
    private final ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    private final CustomerIndexes indexes = mock(CustomerIndexes.class);
    private final CustomerPersistenceMapperImpl mapper = new CustomerPersistenceMapperImpl();
    private final CustomerReactiveRepositoryAdapter adapter = new CustomerReactiveRepositoryAdapter(
            template, mapper, indexes, () -> Mono.just("SYSTEM"));
    private final UUID id = UUID.randomUUID();
    private CustomerDocument document() { return new CustomerDocument(id.toString(), "sokha", "Chan", "Sokha",
            "sokha@example.com", "+85512345678", kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus.ACTIVE); }

    @Test void insertionIsLazyAndPropagatesTheSavedIdentity() {
        when(indexes.customers()).thenReturn(Mono.empty());
        when(template.findById(id.toString(), CustomerDocument.class)).thenReturn(Mono.empty());
        when(template.insert(any(CustomerDocument.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
        Mono<Customer> result = adapter.save(mapper.toDomain(document()));
        verifyNoInteractions(template);
        StepVerifier.create(result).assertNext(c -> assertEquals(id, c.getId().value())).verifyComplete();
    }
    @Test void neverResurrectsASoftDeletedCustomer() {
        when(indexes.customers()).thenReturn(Mono.empty());
        var deleted = new CustomerDocument(id.toString(), "sokha", "Chan", "Sokha", "sokha@example.com", "+85512345678",
                kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus.DELETED);
        when(template.findById(id.toString(), CustomerDocument.class)).thenReturn(Mono.just(deleted));
        StepVerifier.create(adapter.save(mapper.toDomain(document()))).expectError(CustomerNotFoundException.class).verify();
        verify(template, never()).insert(any(CustomerDocument.class));
    }
    @Test void concurrentDeletionOrEditBecomesAConflict() {
        when(indexes.customers()).thenReturn(Mono.empty());
        when(template.findById(id.toString(), CustomerDocument.class)).thenReturn(Mono.just(document()));
        when(template.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(CustomerDocument.class)))
                .thenReturn(Mono.empty());
        StepVerifier.create(adapter.save(mapper.toDomain(document()))).expectError(ConflictException.class).verify();
        verify(template, never()).insert(any(CustomerDocument.class));
    }
    @Test void propagatesDatabaseReadErrors() {
        var failure = new IllegalStateException("offline");
        when(template.findOne(any(Query.class), eq(CustomerDocument.class))).thenReturn(Mono.error(failure));
        StepVerifier.create(adapter.findById(new CustomerId(id))).expectErrorMatches(e -> e == failure).verify();
    }
    @Test void statisticsMapsEmptyFacetToZero() {
        var facet = new Document("totals", List.of()).append("statuses", List.of())
                .append("segments", List.of()).append("combined", List.of());
        when(template.aggregate(any(Aggregation.class), eq("customers"), eq(Document.class))).thenReturn(Flux.just(facet));
        StepVerifier.create(adapter.statistics(new FilterRequest(List.of(), null, 0, 20, "createdAt", "desc")))
                .assertNext(s -> { assertEquals(0, s.total()); assertTrue(s.bySegment().isEmpty()); }).verifyComplete();
    }
}
