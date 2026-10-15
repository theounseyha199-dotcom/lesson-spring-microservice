package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import kh.edu.istad.platform.customer.persistence.repository.CustomerMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerMongoRepositoryAdapter implements CustomerRepository {

    private final CustomerMongoRepository customerMongoRepository;

    @Override
    public Customer save(Customer customer) {
        CustomerDocument document = new CustomerDocument(
                customer.getId().value().toString(),
                customer.getUsername(),
                customer.getFamilyName(),
                customer.getGivenName(),
                customer.getEmail().value(),
                customer.getPhoneNumber().value(),
                customer.getStatus());
        customerMongoRepository.save(document).block();
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return customerMongoRepository.findById(id.value().toString())
                .map(document -> Customer.builder()
                        .id(new CustomerId(UUID.fromString(document.customerId())))
                        .username(document.username())
                        .familyName(document.familyName())
                        .givenName(document.givenName())
                        .email(new Email(document.email()))
                        .phoneNumber(new PhoneNumber(document.phoneNumber()))
                        .status(document.status())
                        .build())
                .blockOptional();
    }
}
