package kh.edu.istad.platform.customer.domain.port.out;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// All persistence operations remain lazy and nonblocking through the application boundary.
public interface CustomerRepository {
    Mono<Customer> save(Customer customer);
    Mono<Customer> findById(CustomerId id);
    Flux<Customer> findAll();
    Flux<Customer> findAll(FilterRequest filter);
    Mono<Boolean> deleteById(CustomerId customerId);
    Mono<CustomerStatistics> statistics(FilterRequest filter);
}
