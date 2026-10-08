package istad.edu.platform.customer.service;

import istad.edu.platform.customer.entity.Customer;
import istad.edu.platform.customer.event.CustomerDeactivateEvent;
import istad.edu.platform.customer.event.CustomerInitiatedEvent;
import istad.edu.platform.customer.event.CustomerUpdateEvent;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CustomerDomainServiceImpl implements CustomerDomainService{

    @Override
    public CustomerInitiatedEvent initiateCustomer(Customer customer) {
        customer.initiateCustomer();
        return new CustomerInitiatedEvent(customer, ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Override
    public CustomerUpdateEvent updateCustomer(Customer customer) {
        customer.updateCustomer(customer.getFamilyName(),customer.getGivenName());
        return new CustomerUpdateEvent(customer , ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Override
    public CustomerDeactivateEvent deactivateCustomer(Customer customer) {
        customer.deactivateCustomer();
        return new CustomerDeactivateEvent(customer);
    }
}
