package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerCommand;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainServiceImpl;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateCustomerUseCaseTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CreateCustomerUseCase useCase =
            new CreateCustomerUseCase(new CustomerDomainServiceImpl(), repository, new SegmentCustomerUseCase(mock(kh.edu.istad.platform.customer.domain.port.out.SegmentRepository.class)));
    private final InitiateCustomerCommand command = new InitiateCustomerCommand(
            "sokha", "Chan", "Sokha", "sokha@example.com", "+85512345678");

    @Test
    void savesAnActiveCustomerWithAnIdAndReturnsItsDetails() {
        when(repository.save(any(Customer.class))).thenAnswer(call -> reactor.core.publisher.Mono.just(call.getArgument(0)));

        var result = useCase.execute(command).block();

        var saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());
        assertNotNull(saved.getValue().getId());
        assertEquals(CustomerStatus.ACTIVE, saved.getValue().getStatus());
        assertEquals(saved.getValue().getId().value(), result.customerId());
        assertEquals(command.username(), result.username());
        assertEquals(command.familyName(), result.familyName());
        assertEquals(command.givenName(), result.givenName());
        assertEquals(command.email(), result.email());
        assertEquals(command.phoneNumber(), result.phoneNumber());
    }

    @Test
    void propagatesSaveFailure() {
        var failure = new IllegalStateException("Database unavailable");
        when(repository.save(any(Customer.class))).thenReturn(reactor.core.publisher.Mono.error(failure));

        assertSame(failure, assertThrows(IllegalStateException.class, () -> useCase.execute(command).block()));
    }
}
