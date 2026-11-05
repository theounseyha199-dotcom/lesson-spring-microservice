package kh.edu.istad.platform.order.persistence.mapper;

import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.entity.OrderItem;
import kh.edu.istad.common.domain.valueobject.OrderStatus;
import kh.edu.istad.platform.order.persistence.entity.OrderDocument;
import kh.edu.istad.platform.order.persistence.entity.OrderItemDocument;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {
    public OrderDocument toEntity(Order order) {
        var items = order.items().stream().map(item -> new OrderItemDocument(
                item.productId(), item.quantity(), item.unitPrice())).toList();
        return new OrderDocument(order.id(), order.customerId(), order.businessId(),
                order.deliveryAddress(), order.total(), order.status().name(), order.createdAt(), items);
    }

    public Order toDomain(OrderDocument entity) {
        var items = entity.getItems().stream().map(item -> new OrderItem(
                item.getProductId(), item.getQuantity(), item.getUnitPrice())).toList();
        return new Order(entity.getId(), entity.getCustomerId(), entity.getBusinessId(),
                entity.getDeliveryAddress(), items, OrderStatus.valueOf(entity.getStatus()), entity.getCreatedAt());
    }
}
