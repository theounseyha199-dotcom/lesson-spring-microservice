package kh.edu.istad.platform.order.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(unitPrice, "unitPrice is required");
        if (quantity <= 0 || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Quantity and unit price must be positive");
        }
    }
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
