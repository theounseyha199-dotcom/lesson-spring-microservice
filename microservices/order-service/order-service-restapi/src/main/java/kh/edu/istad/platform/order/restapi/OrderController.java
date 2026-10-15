package kh.edu.istad.platform.order.restapi;

import kh.edu.istad.platform.order.application.CreateOrderCommand;
import kh.edu.istad.platform.order.application.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrder;
    public OrderController(CreateOrderUseCase createOrder) { this.createOrder = createOrder; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        var items = request.items().stream().map(i ->
                new CreateOrderCommand.Item(i.productId(), i.quantity(), i.unitPrice())).toList();
        var order = createOrder.execute(new CreateOrderCommand(request.customerId(),
                request.businessId(), request.deliveryAddress(), items));
        return new OrderResponse(order.id(), order.customerId(), order.businessId(),
                order.total(), order.status().name(), order.createdAt());
    }
    public record OrderResponse(UUID orderId, UUID customerId, UUID businessId,
                                BigDecimal total, String status, Instant createdAt) {}
}
