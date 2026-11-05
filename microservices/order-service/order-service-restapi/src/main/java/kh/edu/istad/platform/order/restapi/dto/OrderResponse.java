package kh.edu.istad.platform.order.restapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(UUID orderId, UUID customerId, UUID businessId,
                            BigDecimal total, String status, Instant createdAt) {}
