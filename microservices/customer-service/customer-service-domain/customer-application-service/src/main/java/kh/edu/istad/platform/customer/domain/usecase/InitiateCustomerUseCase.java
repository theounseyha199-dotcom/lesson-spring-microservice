package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerCommand;
import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitiateCustomerUseCase {

    private final CreateCustomerUseCase createCustomerUseCase;

    public reactor.core.publisher.Mono<InitiateCustomerResult> execute(InitiateCustomerCommand command) {
        return createCustomerUseCase.execute(command);
    }

}
