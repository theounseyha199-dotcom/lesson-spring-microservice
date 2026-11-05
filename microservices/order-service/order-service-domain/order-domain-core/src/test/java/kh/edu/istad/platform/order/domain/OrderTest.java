package kh.edu.istad.platform.order.domain;

import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.entity.OrderItem;
import kh.edu.istad.platform.order.domain.entity.Product;
import kh.edu.istad.platform.order.domain.entity.Customer;
import kh.edu.istad.platform.order.domain.entity.Business;
import kh.edu.istad.platform.order.domain.exception.OrderDomainException;
import kh.edu.istad.common.domain.valueobject.BusinessId;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.common.domain.valueobject.Money;
import kh.edu.istad.common.domain.valueobject.OrderId;
import kh.edu.istad.common.domain.valueobject.ProductId;
import kh.edu.istad.common.domain.valueobject.OrderStatus;
import kh.edu.istad.platform.order.domain.valueobject.StreetAddress;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @Test void approvesAndCancelsOnlyFromAllowedStates() {
        var id = UUID.randomUUID();
        var item = new OrderItem(UUID.randomUUID(), 1, new BigDecimal("3.25"));
        var approved = new Order(id, id, id, "Phnom Penh", List.of(item), OrderStatus.PAID, Instant.now());
        approved.approve();
        assertEquals(OrderStatus.APPROVED, approved.status());
        assertThrows(OrderDomainException.class, approved::cancel);

        var cancelling = new Order(id, id, id, "Phnom Penh", List.of(item), OrderStatus.PAID, Instant.now());
        cancelling.initCancel(List.of("Payment reversal requested"));
        assertEquals(OrderStatus.CANCELLING, cancelling.status());
        assertEquals(List.of("Payment reversal requested"), cancelling.getFailureMessage());
        cancelling.cancel();
        assertEquals(OrderStatus.CANCELLED, cancelling.status());
    }

    @Test void rejectsAChangedProductPrice() {
        var id = UUID.randomUUID();
        var item = new OrderItem(UUID.randomUUID(), 1, new BigDecimal("3.25"));
        var order = new Order(id, id, id, "Phnom Penh", List.of(item), OrderStatus.PENDING, Instant.now());
        item.getProduct().updateConfirmedNameAndPrice("Item", new Money(new BigDecimal("4.00")));

        assertThrows(OrderDomainException.class, order::validateOrder);
    }

    @Test void initializesAndValidatesOrder() {
        var id = UUID.randomUUID();
        var business = Business.Builder.builder().id(new BusinessId(id)).build();
        var item = new OrderItem(UUID.randomUUID(), 1, new BigDecimal("3.25"));
        var order = Order.Builder.builder()
                .customerId(new CustomerId(id))
                .businessId(business.getId())
                .shippingAddress(new StreetAddress("Phnom Penh"))
                .items(List.of(item))
                .createdAt(Instant.now())
                .build();

        order.initializeOrder();

        assertDoesNotThrow(order::validateOrder);
        assertNotNull(order.id());
        assertNotNull(order.getTrackingId().value());
        assertEquals(OrderStatus.PENDING, order.status());
        order.pay();
        assertEquals(OrderStatus.PAID, order.status());
        assertThrows(OrderDomainException.class, order::pay);
    }

    @Test void buildsOrderAndCustomerWithTypedIds() {
        var id = UUID.randomUUID();
        var item = new OrderItem(UUID.randomUUID(), 2, new BigDecimal("3.25"));
        var customer = Customer.Builder.builder()
                .id(new CustomerId(id))
                .userName("customer")
                .build();
        var order = Order.Builder.builder()
                .id(new OrderId(id))
                .customerId(customer.getId())
                .businessId(new BusinessId(id))
                .shippingAddress(new StreetAddress("Phnom Penh"))
                .items(List.of(item))
                .orderStatus(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        assertEquals(id, order.id());
        assertEquals(new BigDecimal("6.50"), order.total());
        assertEquals(OrderStatus.PENDING, order.status());
    }

    @Test void buildsOrderItemWithCalculatedSubtotal() {
        var productId = UUID.randomUUID();
        var product = Product.Builder.builder()
                .id(new ProductId(productId))
                .name("Item")
                .price(new Money(new BigDecimal("2.50")))
                .build();
        var item = OrderItem.Builder.builder()
                .product(product)
                .quantity(3)
                .price(product.getPrice())
                .build();

        assertEquals(productId, item.productId());
        assertEquals(new BigDecimal("7.50"), item.subtotal());
        assertNotNull(item.getId());
    }

    @Test void calculatesTotalAndRejectsEmptyItems() {
        var item = new OrderItem(UUID.randomUUID(), 3, new BigDecimal("2.50"));
        var id = UUID.randomUUID();
        var order = new Order(id, id, id, "Phnom Penh", List.of(item), OrderStatus.PENDING, Instant.now());
        assertEquals(new BigDecimal("7.50"), order.total());
        assertThrows(IllegalArgumentException.class, () ->
                new Order(id, id, id, "Phnom Penh", List.of(), OrderStatus.PENDING, Instant.now()));
    }
}
