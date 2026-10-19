package kh.edu.istad.platform.order.restapi.mapper;

import kh.edu.istad.platform.order.domain.dto.CreateOrderCommand;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.restapi.dto.CreateOrderRequest;
import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderWebMapper {
    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        var items = request.items().stream().map(item -> item == null ? null :
                new CreateOrderCommand.Item(item.productId(), item.quantity(), item.unitPrice())).toList();
        return new CreateOrderCommand(request.customerId(), request.businessId(), request.deliveryAddress(), items);
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.id(), order.customerId(), order.businessId(),
                order.total(), order.status().name(), order.createdAt());
    }
}
