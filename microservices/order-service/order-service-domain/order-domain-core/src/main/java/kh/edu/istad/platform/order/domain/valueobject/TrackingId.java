package kh.edu.istad.platform.order.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TrackingId(UUID value) {
    public TrackingId {
        Objects.requireNonNull(value, "trackingId is required");
    }
}
