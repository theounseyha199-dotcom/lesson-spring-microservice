package kh.edu.istad.platform.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CreditEntry(UUID id, UUID customerId, BigDecimal amount, TransactionType type) {
    public enum TransactionType { DEBIT, CREDIT }
    public CreditEntry {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(type, "type is required");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Credit entry amount must be positive");
    }
}
