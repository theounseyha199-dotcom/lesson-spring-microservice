package kh.edu.istad.platform.customer.domain.entity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

// Segment deletion preserves identifiers on customers for historical statistics.
/** Named customer grouping with a permanent identity, lifecycle status, and optimistic-lock version. */
public record CustomerSegment(UUID segmentId, String name, String description, Status status,
                              Instant createdAt, Instant updatedAt, Long version) {
    public enum Status { ACTIVE, INACTIVE, DELETED }
    public CustomerSegment {
        if (segmentId == null || status == null) throw new IllegalArgumentException("Segment id and status are required");
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Segment name must contain 1 to 100 characters");
        if (description != null && description.length() > 1000)
            throw new IllegalArgumentException("Segment description must not exceed 1000 characters");
        name = name.trim();
    }
    public String nameKey() { return name.toLowerCase(Locale.ROOT); }
    public CustomerSegment update(String name, String description, Status next) {
        if (status == Status.DELETED || next == null || next == Status.DELETED)
            throw new IllegalArgumentException("Use DELETE to delete a segment; deleted segments cannot be updated");
        return new CustomerSegment(segmentId, name, description, next, createdAt, Instant.now(), version);
    }
    public CustomerSegment deleted() {
        return new CustomerSegment(segmentId, name, description, Status.DELETED, createdAt, Instant.now(), version);
    }
}
