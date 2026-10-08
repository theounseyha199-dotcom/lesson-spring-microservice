package istad.edu.platform.customer.service;

import istad.edu.platform.customer.entity.Customer;
import istad.edu.platform.customer.event.CustomerDeactivateEvent;
import istad.edu.platform.customer.event.CustomerInitiatedEvent;
import istad.edu.platform.customer.event.CustomerUpdateEvent;

public interface CustomerDomainService {
    CustomerInitiatedEvent initiateCustomer(Customer customer);
    CustomerUpdateEvent updateCustomer(Customer customer);
    CustomerDeactivateEvent deactivateCustomer(Customer customer);
}
