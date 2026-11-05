package kh.edu.istad.platform.business.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BusinessTest {
    @Test void requiresValidContactDetails() {
        assertThrows(IllegalArgumentException.class, () ->
                new Business(UUID.randomUUID(), "Shop", "bad-email", "Phnom Penh",
                        Business.Status.ACTIVE, Instant.now()));
    }
}
