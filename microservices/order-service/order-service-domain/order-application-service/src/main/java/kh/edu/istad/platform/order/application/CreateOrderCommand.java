package kh.edu.istad.platform.order.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(UUID customerId, UUID businessId, String deliveryAddress,
                                 List<Item> items) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {}
}
