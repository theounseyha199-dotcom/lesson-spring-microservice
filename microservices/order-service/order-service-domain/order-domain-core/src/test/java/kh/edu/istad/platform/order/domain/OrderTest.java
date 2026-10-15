package kh.edu.istad.platform.order.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @Test void calculatesTotalAndRejectsEmptyItems() {
        var item = new OrderItem(UUID.randomUUID(), 3, new BigDecimal("2.50"));
        var id = UUID.randomUUID();
        var order = new Order(id, id, id, "Phnom Penh", List.of(item), Order.Status.PENDING, Instant.now());
        assertEquals(new BigDecimal("7.50"), order.total());
        assertThrows(IllegalArgumentException.class, () ->
                new Order(id, id, id, "Phnom Penh", List.of(), Order.Status.PENDING, Instant.now()));
    }
}
