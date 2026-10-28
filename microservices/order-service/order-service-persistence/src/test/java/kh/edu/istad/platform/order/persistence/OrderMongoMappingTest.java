package kh.edu.istad.platform.order.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kh.edu.istad.platform.order.persistence.entity.OrderDocument;
import kh.edu.istad.platform.order.persistence.entity.OrderItemDocument;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import static org.assertj.core.api.Assertions.assertThat;

class OrderMongoMappingTest {
    @Test
    void roundTripsIdentifiersMoneyAndOrderedItems() throws Exception {
        var conversions = MongoCustomConversions.create(adapter -> {});
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        var first = new OrderItemDocument(UUID.randomUUID(), 2, new BigDecimal("5.1234"));
        var second = new OrderItemDocument(UUID.randomUUID(), 1, new BigDecimal("1.0001"));
        var original = new OrderDocument(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Phnom Penh", new BigDecimal("11.2469"), "PENDING", Instant.parse("2026-10-27T00:00:00Z"),
                List.of(first, second));
        var bson = new Document();
        converter.write(original, bson);
        assertThat(bson.get("total")).isInstanceOf(Decimal128.class);
        var restored = converter.read(OrderDocument.class, bson);
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}
