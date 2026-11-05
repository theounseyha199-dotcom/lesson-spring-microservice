package kh.edu.istad.platform.payment.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentCommand(UUID orderId, UUID customerId, BigDecimal amount) {}
