package kh.edu.istad.platform.payment.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    @Test void createsPendingAndCannotCompleteTwice() {
        var payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"));
        assertEquals(Payment.Status.PENDING, payment.status());
        var completed = payment.complete();
        assertEquals(Payment.Status.COMPLETED, completed.status());
        assertEquals(2, completed.history().size());
        assertThrows(IllegalStateException.class, completed::complete);
    }
}
