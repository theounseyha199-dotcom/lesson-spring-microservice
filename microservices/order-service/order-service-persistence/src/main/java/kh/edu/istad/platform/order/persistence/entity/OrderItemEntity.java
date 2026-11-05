package kh.edu.istad.platform.order.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

//    private Integer Id;
    @Id
    @Embedded.Nullable
    private OrderItemEntityId orderItemEntityId;
    private BigDecimal price;
    private UUID productId;
    private Integer quantity;
    private BigDecimal subTotal;
//    private UUID orderId;
}
