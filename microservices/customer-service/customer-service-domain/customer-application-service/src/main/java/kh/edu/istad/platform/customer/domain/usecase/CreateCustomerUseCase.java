package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import kh.edu.istad.platform.customer.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Validates and initializes an active customer before persisting the customer and its optional segment. */
@Component
@RequiredArgsConstructor
public class CreateCustomerUseCase {
    private final CustomerDomainService customerDomainService;
    private final CustomerRepository customerRepository;
    private final SegmentCustomerUseCase segmentCustomerUseCase;
    public Mono<InitiateCustomerResult> execute(InitiateCustomerCommand command) {
        return Mono.defer(() -> {
            Customer customer = Customer.builder().username(command.username()).familyName(command.familyName())
                    .givenName(command.givenName()).email(new Email(command.email()))
                    .phoneNumber(new PhoneNumber(command.phoneNumber())).segmentId(command.segmentId()).build();
            customerDomainService.initiateCustomer(customer);
            return segmentCustomerUseCase.requireAssignable(command.segmentId()).then(customerRepository.save(customer))
                    .map(c -> new InitiateCustomerResult(c.getId().value(), c.getUsername(), c.getFamilyName(),
                            c.getGivenName(), c.getEmail().value(), c.getPhoneNumber().value(), c.getSegmentId()));
        });
    }
}
