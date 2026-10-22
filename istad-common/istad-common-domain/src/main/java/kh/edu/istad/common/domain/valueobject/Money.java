package kh.edu.istad.common.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

//150$ new Money(150)
//100$ new Money(100)
public record Money(
        BigDecimal amount
) {
    public void isGreaterThanZero() {
        if (!(amount.compareTo(BigDecimal.ZERO) > 0)) {
            System.out.println("Money is not greater than zero");
            throw new RuntimeException("Money is  not grater than zero");
        }
    }
    //more logic

    public boolean isGreaterThan(Money other) {
        return this.amount != null && other.amount != null && this.amount.compareTo(other.amount) > 0;
    }


    public Money add(Money money) {
        BigDecimal newAmount = this.amount.add(money.amount);
        return new Money(setScale(newAmount));
    }

    private BigDecimal setScale(BigDecimal inputAmount) {
        return inputAmount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Money subtract(Money money) {
        BigDecimal newAmount = this.amount.subtract(money.amount);
        return new Money(setScale(newAmount));
    }

    public Money multiply(Integer multiplier) {
        BigDecimal newAmount = this.amount.multiply(BigDecimal.valueOf(multiplier));
        return new Money(setScale(newAmount));
    }



}