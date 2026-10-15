package kh.edu.istad.platform.payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreditHistory(UUID creditEntryId, String action, Instant createdAt) {
    public CreditHistory {
        Objects.requireNonNull(creditEntryId, "creditEntryId is required");
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Action is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }
}
