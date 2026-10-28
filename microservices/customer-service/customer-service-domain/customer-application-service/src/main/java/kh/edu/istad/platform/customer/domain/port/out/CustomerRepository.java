package kh.edu.istad.platform.customer.domain.port.out;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;

import java.util.Optional;

public interface CustomerRepository {
    java.util.List<Customer> findAll();

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);
}
