package kh.edu.istad.platform.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {
    public enum Status { PENDING, PAID, CANCELLED }
    private final UUID id;
    private final UUID customerId;
    private final UUID businessId;
    private final String deliveryAddress;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private final Status status;
    private final Instant createdAt;

    public Order(UUID id, UUID customerId, UUID businessId, String deliveryAddress,
                 List<OrderItem> items, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.businessId = Objects.requireNonNull(businessId, "businessId is required");
        if (deliveryAddress == null || deliveryAddress.isBlank())
            throw new IllegalArgumentException("Delivery address is required");
        if (items == null || items.isEmpty() || items.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("At least one valid order item is required");
        this.deliveryAddress = deliveryAddress.trim();
        this.items = List.copyOf(items);
        this.total = this.items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }
    public UUID id() { return id; }
    public UUID customerId() { return customerId; }
    public UUID businessId() { return businessId; }
    public String deliveryAddress() { return deliveryAddress; }
    public List<OrderItem> items() { return items; }
    public BigDecimal total() { return total; }
    public Status status() { return status; }
    public Instant createdAt() { return createdAt; }
}
