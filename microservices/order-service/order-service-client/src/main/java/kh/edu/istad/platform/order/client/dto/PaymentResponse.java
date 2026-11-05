package kh.edu.istad.platform.order.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount, String status) {}
