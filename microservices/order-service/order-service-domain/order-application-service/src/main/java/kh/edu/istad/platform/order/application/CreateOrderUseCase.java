package kh.edu.istad.platform.order.application;

import kh.edu.istad.platform.order.domain.Order;
import kh.edu.istad.platform.order.domain.OrderItem;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class CreateOrderUseCase {
    private final OrderRepository repository;
    public CreateOrderUseCase(OrderRepository repository) { this.repository = repository; }
    public Order execute(CreateOrderCommand command) {
        if (command == null || command.items() == null)
            throw new IllegalArgumentException("Order items are required");
        var items = command.items().stream().map(i -> {
            if (i == null) throw new IllegalArgumentException("Order item is required");
            return new OrderItem(i.productId(), i.quantity(), i.unitPrice());
        }).toList();
        var order = new Order(UUID.randomUUID(), command.customerId(), command.businessId(),
                command.deliveryAddress(), items, Order.Status.PENDING, Instant.now());
        return repository.save(order);
    }
}
