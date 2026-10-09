package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.persistence.entity.CustomerEntity;
import kh.edu.istad.platform.customer.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository customerJpaRepository;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = new CustomerEntity(
                customer.getId().value(), customer.getUsername(), customer.getFamilyName(),
                customer.getGivenName(), customer.getEmail().value(),
                customer.getPhoneNumber().value(), customer.getStatus());
        customerJpaRepository.save(entity);
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return customerJpaRepository.findById(id.value()).map(entity -> Customer.builder()
                .id(new CustomerId(entity.getCustomerId()))
                .username(entity.getUsername())
                .familyName(entity.getFamilyName())
                .givenName(entity.getGivenName())
                .email(new Email(entity.getEmail()))
                .phoneNumber(new PhoneNumber(entity.getPhoneNumber()))
                .status(entity.getStatus())
                .build());
    }
}
