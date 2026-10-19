package kh.edu.istad.platform.order.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    @Column(name = "order_id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_number")
    private List<OrderItemJpaValue> items = new ArrayList<>();

    protected OrderJpaEntity() {} // JPA

    public OrderJpaEntity(UUID id, UUID customerId, UUID businessId, String deliveryAddress,
                          BigDecimal total, String status, Instant createdAt, List<OrderItemJpaValue> items) {
        this.id = id;
        this.customerId = customerId;
        this.businessId = businessId;
        this.deliveryAddress = deliveryAddress;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getBusinessId() { return businessId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItemJpaValue> getItems() { return items; }
}
