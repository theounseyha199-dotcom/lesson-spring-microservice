package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;
/** Soft-deletes exactly one visible customer; missing and already deleted customers return not found. */
@Component
@RequiredArgsConstructor
public class DeleteCustomerUseCase {
    private final CustomerRepository customerRepository;
    public Mono<Void> execute(UUID id) {
        return customerRepository.deleteById(new CustomerId(id)).flatMap(deleted -> deleted
                ? Mono.empty() : Mono.error(new CustomerNotFoundException(id)));
    }
}
