package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.persistence.entity.CustomerEntity;
import kh.edu.istad.platform.customer.persistence.repository.CustomerJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerRepositoryAdapterTest {

    private final CustomerJpaRepository jpaRepository = mock(CustomerJpaRepository.class);
    private final CustomerRepositoryAdapter adapter = new CustomerRepositoryAdapter(jpaRepository);

    @Test
    void savesDomainCustomerAsJpaEntity() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(new CustomerId(id))
                .username("sokha")
                .familyName("Chan")
                .givenName("Sokha")
                .email(new Email("sokha@example.com"))
                .phoneNumber(new PhoneNumber("+85512345678"))
                .status(CustomerStatus.ACTIVE)
                .build();

        assertSame(customer, adapter.save(customer));
        org.mockito.ArgumentCaptor<CustomerEntity> saved =
                org.mockito.ArgumentCaptor.forClass(CustomerEntity.class);
        verify(jpaRepository).save(saved.capture());
        assertEquals(id, saved.getValue().getCustomerId());
        assertEquals("sokha@example.com", saved.getValue().getEmail());
        assertEquals(CustomerStatus.ACTIVE, saved.getValue().getStatus());
    }

    @Test
    void loadsJpaEntityAsDomainCustomer() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(any(UUID.class))).thenReturn(Optional.of(new CustomerEntity(
                id, "sokha", "Chan", "Sokha", "sokha@example.com", "+85512345678", CustomerStatus.INACTIVE)));

        Customer customer = adapter.findById(new CustomerId(id)).orElseThrow();

        verify(jpaRepository).findById(id);
        assertEquals(id, customer.getId().value());
        assertEquals("sokha", customer.getUsername());
        assertEquals("Chan", customer.getFamilyName());
        assertEquals("Sokha", customer.getGivenName());
        assertEquals("sokha@example.com", customer.getEmail().value());
        assertEquals("+85512345678", customer.getPhoneNumber().value());
        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
    }
}
