package kh.edu.istad.platform.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Payment {
    public enum Status { PENDING, COMPLETED, FAILED, CANCELLED }
    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final Status status;
    private final CreditEntry creditEntry;
    private final List<CreditHistory> history;
    private final Instant createdAt;

    public Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, Status status,
                   CreditEntry creditEntry, List<CreditHistory> history, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.orderId = Objects.requireNonNull(orderId, "orderId is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Payment amount must be positive");
        this.amount = amount;
        this.status = Objects.requireNonNull(status, "status is required");
        this.creditEntry = Objects.requireNonNull(creditEntry, "creditEntry is required");
        this.history = List.copyOf(Objects.requireNonNull(history, "history is required"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }
    public static Payment create(UUID orderId, UUID customerId, BigDecimal amount) {
        var entry = new CreditEntry(UUID.randomUUID(), customerId, amount, CreditEntry.TransactionType.DEBIT);
        var now = Instant.now();
        return new Payment(UUID.randomUUID(), orderId, customerId, amount, Status.PENDING,
                entry, List.of(new CreditHistory(entry.id(), "CREATED", now)), now);
    }
    public Payment complete() { return changeStatus(Status.COMPLETED); }
    public Payment fail() { return changeStatus(Status.FAILED); }
    public Payment cancel() { return changeStatus(Status.CANCELLED); }
    private Payment changeStatus(Status next) {
        if (status != Status.PENDING) throw new IllegalStateException("Only pending payments can change status");
        var updated = new java.util.ArrayList<>(history);
        updated.add(new CreditHistory(creditEntry.id(), next.name(), Instant.now()));
        return new Payment(id, orderId, customerId, amount, next, creditEntry, updated, createdAt);
    }
    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public UUID customerId() { return customerId; }
    public BigDecimal amount() { return amount; }
    public Status status() { return status; }
    public CreditEntry creditEntry() { return creditEntry; }
    public List<CreditHistory> history() { return history; }
    public Instant createdAt() { return createdAt; }
}
