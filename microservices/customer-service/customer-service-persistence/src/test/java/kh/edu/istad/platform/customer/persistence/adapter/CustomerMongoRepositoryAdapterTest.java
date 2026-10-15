package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import kh.edu.istad.platform.customer.persistence.repository.CustomerMongoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerMongoRepositoryAdapterTest {
    private final CustomerMongoRepository mongoRepository = mock(CustomerMongoRepository.class);
    private final CustomerMongoRepositoryAdapter adapter = new CustomerMongoRepositoryAdapter(mongoRepository);

    @Test
    void savesAndLoadsCustomerWithUuidAsDocumentId() {
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
        when(mongoRepository.save(any(CustomerDocument.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        assertSame(customer, adapter.save(customer));
        ArgumentCaptor<CustomerDocument> saved = ArgumentCaptor.forClass(CustomerDocument.class);
        verify(mongoRepository).save(saved.capture());
        assertEquals(id.toString(), saved.getValue().customerId());
        assertEquals("sokha@example.com", saved.getValue().email());
        when(mongoRepository.findById(id.toString())).thenReturn(Mono.just(saved.getValue()));

        Customer loaded = adapter.findById(new CustomerId(id)).orElseThrow();
        assertEquals(id, loaded.getId().value());
        assertEquals(CustomerStatus.ACTIVE, loaded.getStatus());
        assertEquals("sokha@example.com", loaded.getEmail().value());
    }
}
