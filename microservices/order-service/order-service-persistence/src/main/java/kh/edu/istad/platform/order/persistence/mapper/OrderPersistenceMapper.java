package kh.edu.istad.platform.order.persistence.mapper;

import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.entity.OrderItem;
import kh.edu.istad.platform.order.persistence.entity.OrderJpaEntity;
import kh.edu.istad.platform.order.persistence.entity.OrderItemJpaValue;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {
    public OrderJpaEntity toEntity(Order order) {
        var items = order.items().stream().map(item -> new OrderItemJpaValue(
                item.productId(), item.quantity(), item.unitPrice())).toList();
        return new OrderJpaEntity(order.id(), order.customerId(), order.businessId(),
                order.deliveryAddress(), order.total(), order.status().name(), order.createdAt(), items);
    }

    public Order toDomain(OrderJpaEntity entity) {
        var items = entity.getItems().stream().map(item -> new OrderItem(
                item.getProductId(), item.getQuantity(), item.getUnitPrice())).toList();
        return new Order(entity.getId(), entity.getCustomerId(), entity.getBusinessId(),
                entity.getDeliveryAddress(), items, Order.Status.valueOf(entity.getStatus()), entity.getCreatedAt());
    }
}
