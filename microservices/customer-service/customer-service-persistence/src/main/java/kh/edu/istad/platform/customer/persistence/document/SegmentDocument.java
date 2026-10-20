package kh.edu.istad.platform.customer.persistence.document;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

// Optimistic locking rejects concurrent segment edits instead of silently overwriting them.
@Document("customer_segments")
public record SegmentDocument(@Id String id, String name, String nameKey, String description,
                              CustomerSegment.Status status, Instant createdAt, Instant updatedAt,
                              @Version Long version) {}
