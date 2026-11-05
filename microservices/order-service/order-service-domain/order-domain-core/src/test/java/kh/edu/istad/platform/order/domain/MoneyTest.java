package kh.edu.istad.platform.order.domain;

import kh.edu.istad.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void classifiesPositiveNegativeZeroAndUnknownAmountsWithoutThrowing() {
        Money positive = new Money(new BigDecimal("12.50"));
        Money negative = new Money(new BigDecimal("-12.50"));
        Money unknown = new Money(null);

        assertTrue(positive.isGreaterThanZero());
        assertFalse(positive.isLessThanZero());
        assertTrue(negative.isLessThanZero());
        assertFalse(negative.isGreaterThanZero());
        assertFalse(Money.ZERO.isGreaterThanZero());
        assertFalse(Money.ZERO.isLessThanZero());
        assertFalse(unknown.isGreaterThanZero());
        assertFalse(unknown.isLessThanZero());
    }
}
