package kh.edu.istad.platform.order.restapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(@NotNull UUID customerId, @NotNull UUID businessId,
                                 @NotBlank String deliveryAddress,
                                 @NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull UUID productId, @Min(1) int quantity,
                       @NotNull @DecimalMin("0.01") BigDecimal unitPrice) {}
}
