package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.AggregateRoot;
import kh.edu.istad.common.domain.valueobject.*;
import kh.edu.istad.platform.order.domain.valueobject.StreetAddress;
import kh.edu.istad.platform.order.domain.valueobject.TrackingId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order extends AggregateRoot<OrderId> {
    private final CustomerId customerId;
    private final BusinessId businessId;
    private final StreetAddress shippingAddress;
    private final Money price;
    private final List<OrderItem> items;
    private final OrderStatus orderStatus;
    private final Instant createdAt;
    private  TrackingId trackingId;
    private List<String> failureMessage;
    private Long version;

    public Order(UUID id, UUID customerId, UUID businessId, String deliveryAddress,
                 List<OrderItem> items, OrderStatus status, Instant createdAt) {
        setId(new OrderId(Objects.requireNonNull(id, "id is required")));
        this.customerId = new CustomerId(Objects.requireNonNull(customerId, "customerId is required"));
        this.businessId = new BusinessId(Objects.requireNonNull(businessId, "businessId is required"));
        this.shippingAddress = new StreetAddress(deliveryAddress);
        if (items == null || items.isEmpty() || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("At least one valid order item is required");
        }
        this.items = List.copyOf(items);
        this.price = new Money(this.items.stream().map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        this.orderStatus = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    private Order(Builder builder) {
        setId(Objects.requireNonNull(builder.id, "id is required"));
        customerId = Objects.requireNonNull(builder.customerId, "customerId is required");
        businessId = Objects.requireNonNull(builder.businessId, "businessId is required");
        shippingAddress = Objects.requireNonNull(builder.shippingAddress, "shippingAddress is required");
        if (builder.items == null || builder.items.isEmpty() || builder.items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("At least one valid order item is required");
        }
        items = List.copyOf(builder.items);
        Money calculatedPrice = new Money(items.stream().map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (builder.price != null && builder.price.amount().compareTo(calculatedPrice.amount()) != 0) {
            throw new IllegalArgumentException("Price must equal the sum of item subtotals");
        }
        price = calculatedPrice;
        orderStatus = Objects.requireNonNull(builder.orderStatus, "orderStatus is required");
        createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        trackingId = builder.trackingId;
        failureMessage = builder.failureMessage == null ? List.of() : List.copyOf(builder.failureMessage);
        version = builder.version;
    }

    public UUID id() { return getId().Id(); }
    public UUID customerId() { return customerId.value(); }
    public UUID businessId() { return businessId.value(); }
    public String deliveryAddress() { return shippingAddress.value(); }
    public List<OrderItem> items() { return items; }
    public BigDecimal total() { return price.amount(); }
    public OrderStatus status() { return orderStatus; }
    public Instant createdAt() { return createdAt; }


    public static final class Builder {
        private OrderId id;
        private CustomerId customerId;
        private BusinessId businessId;
        private StreetAddress shippingAddress;
        private Money price;
        private List<OrderItem> items;
        private OrderStatus orderStatus;
        private Instant createdAt;
        private TrackingId trackingId;
        private List<String> failureMessage;
        private Long version;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(OrderId val) {
            id = val;
            return this;
        }

        public Builder customerId(CustomerId val) {
            customerId = val;
            return this;
        }

        public Builder businessId(BusinessId val) {
            businessId = val;
            return this;
        }

        public Builder shippingAddress(StreetAddress val) {
            shippingAddress = val;
            return this;
        }

        public Builder price(Money val) {
            price = val;
            return this;
        }

        public Builder items(List<OrderItem> val) {
            items = val;
            return this;
        }

        public Builder orderStatus(OrderStatus val) {
            orderStatus = val;
            return this;
        }

        public Builder createdAt(Instant val) {
            createdAt = val;
            return this;
        }

        public Builder trackingId(TrackingId val) {
            trackingId = val;
            return this;
        }

        public Builder failureMessage(List<String> val) {
            failureMessage = val;
            return this;
        }

        public Builder version(Long val) {
            version = val;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public BusinessId getBusinessId() {
        return businessId;
    }

    public StreetAddress getShippingAddress() {
        return shippingAddress;
    }

    public Money getPrice() {
        return price;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TrackingId getTrackingId() {
        return trackingId;
    }

    public List<String> getFailureMessage() {
        return failureMessage;
    }

    public Long getVersion() {
        return version;
    }
}
