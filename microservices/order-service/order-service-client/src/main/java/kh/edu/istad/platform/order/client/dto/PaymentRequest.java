package kh.edu.istad.platform.order.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID orderId, UUID customerId, BigDecimal amount) {}
