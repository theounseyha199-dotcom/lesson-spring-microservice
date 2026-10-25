package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.AggregateRoot;
import kh.edu.istad.common.domain.valueobject.*;
import kh.edu.istad.platform.order.domain.exception.OrderDomainException;
import kh.edu.istad.platform.order.domain.valueobject.StreetAddress;
import kh.edu.istad.platform.order.domain.valueobject.TrackingId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order extends AggregateRoot<OrderId> {
    private final CustomerId customerId;
    private final BusinessId businessId;
    private final StreetAddress shippingAddress;
    private final Money price;
    private final List<OrderItem> items;
    private OrderStatus orderStatus;
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
        this.failureMessage = new ArrayList<>();
    }

    private Order(Builder builder) {
        if ((builder.id == null) != (builder.orderStatus == null)) {
            throw new IllegalArgumentException("id and orderStatus must both be set or both be absent");
        }
        setId(builder.id);
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
        orderStatus = builder.orderStatus;
        createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        trackingId = builder.trackingId;
        failureMessage = builder.failureMessage == null ? new ArrayList<>() : new ArrayList<>(builder.failureMessage);
        version = builder.version;
    }

    public UUID id() { return getId() == null ? null : getId().Id(); }
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
        private Money price; // orderprice
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
        return List.copyOf(failureMessage);
    }

    public Long getVersion() {
        return version;
    }

    // ...............start domain logic................//

    public void initializeOrder(){
        if (super.getId() != null || orderStatus != null){
            throw new OrderDomainException("order has been initiated");
        }
        validateOrder();
        super.setId(new OrderId(UUID.randomUUID()));
        trackingId = new TrackingId(UUID.randomUUID());
        orderStatus = OrderStatus.PENDING;
    }

    public void validateOrder(){
        if (price == null || !price.isGreaterThan(new Money(BigDecimal.ZERO))){
            throw new OrderDomainException("Price must be greater than zero");
        }

        Money orderSubTotal = items.stream()
                .map(orderItem -> {
                    validateItemPrice(orderItem);
                    return orderItem.getSubTotal();
                })
                .reduce(Money.ZERO, Money::add);
        if (orderSubTotal.amount().compareTo(price.amount()) != 0) {
            throw new OrderDomainException("Order price must equal the sum of item subtotals");
        }
    }

    private void validateItemPrice(OrderItem orderItem){
        if (orderItem.getPrice().amount().compareTo(orderItem.getProduct().getPrice().amount()) != 0){
            throw new OrderDomainException("Order item price must match product price");
        }
    }




    public void pay(){
        if (orderStatus != OrderStatus.PENDING){
            throw new OrderDomainException("Order state is not create");
        }
        orderStatus = OrderStatus.PAID;
    }

    public void approve(){
        if (orderStatus !=  OrderStatus.PAID){
            throw new OrderDomainException("Only a paid order can be approved");
        }
        orderStatus = OrderStatus.APPROVED;
    }



    public void initCancel (){
        initCancel(List.of());
    }

    public void initCancel(List<String> messages){
        if (orderStatus!= OrderStatus.PAID){
            throw new OrderDomainException("Only a paid order can start cancellation");
        }

        orderStatus = OrderStatus.CANCELLING;
        updateFailureMessage(messages);

    }

    private void updateFailureMessage(List<String> messages){
        if (messages != null && !messages.isEmpty()) {
            this.failureMessage.addAll(messages);
        }
    }


    public void cancel(){
        if(orderStatus != OrderStatus.PENDING && orderStatus != OrderStatus.CANCELLING){
            throw new OrderDomainException("Only a pending or cancelling order can be cancelled");
        }

        orderStatus = OrderStatus.CANCELLED;
    }



    // ...............End domain logic................//


}
