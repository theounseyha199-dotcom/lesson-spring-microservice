package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

/** Applies business rules to customer details, status transitions, and segment membership changes. */
@Component
@RequiredArgsConstructor
public class UpdateCustomerUseCase {
    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;
    private final SegmentCustomerUseCase segmentCustomerUseCase;
    private Mono<Customer> get(UUID id) {
        return customerRepository.findById(new CustomerId(id))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(id)));
    }
    public Mono<UpdateCustomerResult> execute(UpdateCustomerCommand command) {
        return get(command.customerId()).flatMap(customer -> {
            customerDomainService.updateCustomer(customer, command.familyName(), command.givenName());
            customer.updateDetails(command.username(), command.email(), command.phoneNumber());
            return customerRepository.save(customer);
        }).map(c -> new UpdateCustomerResult(c.getId().value(), c.getUsername(), c.getFamilyName(), c.getGivenName(),
                c.getEmail().value(), c.getPhoneNumber().value()));
    }
    public Mono<QueryCustomerResult> status(UUID id, CustomerStatus status) {
        return get(id).flatMap(c -> { c.changeStatus(status); return customerRepository.save(c); })
                .map(QueryCustomerUseCase::result);
    }
    public Mono<QueryCustomerResult> segment(UUID id, UUID segmentId) {
        return segmentCustomerUseCase.requireAssignable(segmentId).then(get(id)).flatMap(c -> {
            c.assignSegment(segmentId);
            return customerRepository.save(c);
        }).map(QueryCustomerUseCase::result);
    }
}
