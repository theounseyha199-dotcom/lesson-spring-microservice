package istad.edu.platform.customer.event;

import istad.edu.platform.customer.entity.Customer;
import kh.edu.istad.common.domain.event.DomainEvent;

import java.time.ZonedDateTime;

public class CustomerInitiatedEvent implements DomainEvent<Customer> {
    private final Customer customer;
    private final ZonedDateTime initiatedAt = ZonedDateTime.now();

    public CustomerInitiatedEvent(Customer customer, ZonedDateTime now) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public ZonedDateTime getInitiatedAt() {
        return initiatedAt;
    }
}
