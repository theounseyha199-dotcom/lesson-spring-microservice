package kh.edu.istad.platform.order.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "orders")
public class OrderDocument {
    @Id
    private UUID id;

    private UUID customerId;

    private UUID businessId;

    private String deliveryAddress;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal total;

    private String status;

    private Instant createdAt;

    private List<OrderItemDocument> items = new ArrayList<>();

    protected OrderDocument() {}

    public OrderDocument(UUID id, UUID customerId, UUID businessId, String deliveryAddress,
                          BigDecimal total, String status, Instant createdAt, List<OrderItemDocument> items) {
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
    public List<OrderItemDocument> getItems() { return items; }
}
