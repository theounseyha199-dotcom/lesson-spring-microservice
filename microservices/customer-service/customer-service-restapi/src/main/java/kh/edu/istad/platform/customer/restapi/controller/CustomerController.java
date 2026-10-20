package kh.edu.istad.platform.customer.restapi.controller;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.usecase.*;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.event.CustomerDeactivatedEvent;
import kh.edu.istad.platform.customer.restapi.dto.*;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.*;

/** Customer HTTP boundary: validates requests and returns reactive results without blocking database calls. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
    private final CreateCustomerUseCase createCustomerUseCase;
    private final QueryCustomerUseCase queryCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;
    private final CustomerWebMapper customerWebMapper;

    // Collection responses stay JSON arrays; page and size bound database work and response memory.
    @GetMapping
    public Mono<List<CustomerQueryResponse>> queryCustomers(
            @RequestParam org.springframework.util.MultiValueMap<String, String> parameters,
            @RequestParam(name="search", required=false) String search,
            @RequestParam(name="page", defaultValue="0") int page, @RequestParam(name="size", defaultValue="20") int size,
            @RequestParam(name="sort", defaultValue="createdAt") String sort, @RequestParam(name="direction", defaultValue="desc") String direction) {
        return queryCustomerUseCase.execute(new FilterRequest(parameters.get("filter"), search, page, size, sort, direction))
                .map(customerWebMapper::toResponse).collectList();
    }
    @GetMapping("/{customerId}")
    public Mono<CustomerQueryResponse> get(@PathVariable("customerId") UUID customerId) {
        return queryCustomerUseCase.get(customerId).map(customerWebMapper::toResponse);
    }
    @GetMapping("/statistics")
    public Mono<CustomerStatistics> statistics(@RequestParam org.springframework.util.MultiValueMap<String, String> parameters,
                                               @RequestParam(name="search", required=false) String search) {
        // Statistics intentionally aggregate all matches, independently of list pagination.
        return queryCustomerUseCase.statistics(new FilterRequest(parameters.get("filter"), search, 0, 20, "createdAt", "desc"));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CustomerInitiateResponse> initiateCustomer(@Valid @RequestBody CustomerInitiateRequest request) {
        return createCustomerUseCase.execute(customerWebMapper.toCommand(request)).map(customerWebMapper::toResponse);
    }
    @PutMapping("/{customerId}")
    public Mono<CustomerUpdateResponse> updateCustomer(@PathVariable("customerId") UUID customerId, @Valid @RequestBody CustomerUpdateRequest request) {
        return updateCustomerUseCase.execute(new UpdateCustomerCommand(customerId, request.familyName(), request.givenName(),
                request.username(), request.email(), request.phoneNumber())).map(customerWebMapper::toResponse);
    }
    @PatchMapping("/{customerId}/status")
    public Mono<CustomerQueryResponse> status(@PathVariable("customerId") UUID customerId, @Valid @RequestBody StatusRequest request) {
        return updateCustomerUseCase.status(customerId, request.status()).map(customerWebMapper::toResponse);
    }
    @PutMapping("/{customerId}/segment")
    public Mono<CustomerQueryResponse> segment(@PathVariable("customerId") UUID customerId, @Valid @RequestBody SegmentRequest request) {
        return updateCustomerUseCase.segment(customerId, request.segmentId()).map(customerWebMapper::toResponse);
    }
    @DeleteMapping("/{customerId}/segment")
    public Mono<CustomerQueryResponse> clearSegment(@PathVariable("customerId") UUID customerId) {
        return updateCustomerUseCase.segment(customerId, null).map(customerWebMapper::toResponse);
    }
    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteCustomer(@PathVariable("customerId") UUID customerId) { return deleteCustomerUseCase.execute(customerId); }
    @PutMapping("/{customerId}/deactivate")
    public Mono<CustomerDeactivatedEvent> deactivatedEvent(@PathVariable("customerId") UUID customerId) {
        return deactivateCustomerUseCase.execute(customerId);
    }
    public record StatusRequest(@NotNull CustomerStatus status) {}
    public record SegmentRequest(@NotNull UUID segmentId) {}
}
