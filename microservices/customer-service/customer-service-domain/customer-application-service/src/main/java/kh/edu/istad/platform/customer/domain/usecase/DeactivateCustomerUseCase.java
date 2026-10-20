package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.event.CustomerDeactivatedEvent;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class DeactivateCustomerUseCase {
    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;
    public Mono<CustomerDeactivatedEvent> execute(UUID id) {
        return customerRepository.findById(new CustomerId(id))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(id))).flatMap(customer -> {
                    var event = customerDomainService.deactivateCustomer(customer);
                    return customerRepository.save(customer).thenReturn(event);
                });
    }
}
