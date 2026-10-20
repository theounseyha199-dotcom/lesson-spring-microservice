package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueryCustomerUseCase {
    private final CustomerRepository customerRepository;
    public Flux<QueryCustomerResult> execute() { return customerRepository.findAll().map(QueryCustomerUseCase::result); }
    public Flux<QueryCustomerResult> execute(FilterRequest filter) {
        return customerRepository.findAll(filter).map(QueryCustomerUseCase::result);
    }
    public Mono<QueryCustomerResult> get(UUID id) {
        return customerRepository.findById(new CustomerId(id))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(id))).map(QueryCustomerUseCase::result);
    }
    public Mono<CustomerStatistics> statistics(FilterRequest filter) { return customerRepository.statistics(filter); }
    public static QueryCustomerResult result(Customer c) {
        return new QueryCustomerResult(c.getId().value(), c.getUsername(), c.getFamilyName(), c.getGivenName(),
                c.getEmail().value(), c.getPhoneNumber().value(), c.getStatus(), c.getSegmentId());
    }
}
