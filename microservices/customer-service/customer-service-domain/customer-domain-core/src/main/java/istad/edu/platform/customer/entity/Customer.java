package istad.edu.platform.customer.entity;

import kh.edu.istad.common.domain.valueobject.Money;

import java.math.BigDecimal;

public class Customer {
    Money money = new Money(BigDecimal.valueOf(1000));
    public void getMoney() {
        System.out.println(money);
    }
    public static void main() {
        Customer customer = new Customer();
        customer.getMoney();
    }
}