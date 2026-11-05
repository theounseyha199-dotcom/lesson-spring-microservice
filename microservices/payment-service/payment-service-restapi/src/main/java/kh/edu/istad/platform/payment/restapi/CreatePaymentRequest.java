package kh.edu.istad.platform.payment.restapi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(@NotNull UUID orderId, @NotNull UUID customerId,
                                   @NotNull @DecimalMin("0.01") BigDecimal amount) {}
