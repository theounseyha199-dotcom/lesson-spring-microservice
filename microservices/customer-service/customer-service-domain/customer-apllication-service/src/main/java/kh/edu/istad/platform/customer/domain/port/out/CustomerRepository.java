package kh.edu.istad.platform.customer.domain.port.out;

import istad.edu.platform.customer.entity.Customer;

public interface CustomerRepository {
    Customer save(Customer customer);
}
