package kh.edu.istad.platform.customer.persistence.config;

import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.ReactiveAuditingEntityCallback;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerAuditingTest {

    @Test
    void auditsCreationAndUpdateThroughTheConfiguredMongoCallback() throws Exception {
        var conversions = MongoCustomConversions.create(adapter -> {});
        var mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.setInitialEntitySet(Set.of(CustomerDocument.class));
        mappingContext.afterPropertiesSet();

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("mongoMappingContext", MongoMappingContext.class, () -> mappingContext);
            context.register(AuditorAwareConfig.class, MongoConfig.class);
            context.refresh();

            var handler = context.getBean(ReactiveIsNewAwareAuditingHandler.class);
            var callback = context.getBean(ReactiveAuditingEntityCallback.class);
            Instant creationTime = Instant.parse("2026-01-01T00:00:00Z");
            Instant updateTime = creationTime.plusSeconds(60);
            handler.setDateTimeProvider(() -> Optional.of(creationTime));
            var original = new CustomerDocument(UUID.randomUUID().toString(), "sokha", "Chan", "Sokha",
                    "sokha@example.com", "+85512345678", CustomerStatus.ACTIVE);

            CustomerDocument created = (CustomerDocument) Mono.from(
                    callback.onBeforeConvert(original, "customers")).block();
            assertNotNull(created);
            assertEquals(creationTime, created.createdAt());
            assertEquals(creationTime, created.updatedAt());
            assertEquals("SYSTEM", created.createdBy());
            assertEquals("SYSTEM", created.updatedBy());

            var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
            converter.setCustomConversions(conversions);
            converter.afterPropertiesSet();
            var bson = new Document();
            converter.write(created, bson);
            assertInstanceOf(java.util.Date.class, bson.get("createdAt"));
            assertFalse(bson.containsKey("newCustomer"));
            assertFalse(bson.containsKey("new"));
            CustomerDocument loaded = converter.read(CustomerDocument.class, bson);
            assertFalse(loaded.isNew());

            handler.setDateTimeProvider(() -> Optional.of(updateTime));
            handler.setAuditorAware(() -> Mono.just("UPDATE_USER"));
            CustomerDocument updated = (CustomerDocument) Mono.from(
                    callback.onBeforeConvert(loaded, "customers")).block();
            assertNotNull(updated);
            assertEquals(creationTime, updated.createdAt());
            assertEquals("SYSTEM", updated.createdBy());
            assertEquals(updateTime, updated.updatedAt());
            assertEquals("UPDATE_USER", updated.updatedBy());

            // Existing documents written before auditing remain updatable.
            bson.remove("createdAt");
            bson.remove("updatedAt");
            bson.remove("createdBy");
            bson.remove("updatedBy");
            CustomerDocument legacy = converter.read(CustomerDocument.class, bson);
            assertFalse(legacy.isNew());
            CustomerDocument migrated = (CustomerDocument) Mono.from(
                    callback.onBeforeConvert(legacy, "customers")).block();
            assertNotNull(migrated);
            assertNull(migrated.createdAt());
            assertEquals(updateTime, migrated.updatedAt());
        }
    }
}
