package kh.edu.istad.platform.customer.restapi.controller;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerResult;
import kh.edu.istad.platform.customer.domain.dto.UpdateCustomerCommand;
import kh.edu.istad.platform.customer.domain.dto.UpdateCustomerResult;
import kh.edu.istad.platform.customer.domain.event.CustomerDeactivatedEvent;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.usecase.DeactivateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.InitiateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.UpdateCustomerUseCase;
import kh.edu.istad.platform.customer.restapi.dto.CustomerInitiateRequest;
import kh.edu.istad.platform.customer.restapi.dto.CustomerInitiateResponse;
import kh.edu.istad.platform.customer.restapi.dto.CustomerUpdateRequest;
import kh.edu.istad.platform.customer.restapi.dto.CustomerUpdateResponse;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final InitiateCustomerUseCase initiateCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;
    private final CustomerWebMapper customerWebMapper;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomerInitiateResponse initiateCustomer(
            @Valid @RequestBody CustomerInitiateRequest customerInitiateRequest
            ) {

        InitiateCustomerResult result = initiateCustomerUseCase.execute(
                customerWebMapper.toCommand(customerInitiateRequest)
        );
        return customerWebMapper.toResponse(result);
    }

    @PutMapping("/{customerId}")
    public CustomerUpdateResponse updateCustomer(
            @PathVariable("customerId") UUID customerId,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        UpdateCustomerResult result = updateCustomerUseCase.execute(
                new UpdateCustomerCommand(customerId, request.familyName(), request.givenName())
        );
        return customerWebMapper.toResponse(result);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void customerNotFound() {
    }

    @PutMapping("/{customerId}/deactivate")
    public CustomerDeactivatedEvent deactivatedEvent(
            @PathVariable("customerId") UUID customerId
    ) {
        return deactivateCustomerUseCase.execute(customerId);
    }

}
