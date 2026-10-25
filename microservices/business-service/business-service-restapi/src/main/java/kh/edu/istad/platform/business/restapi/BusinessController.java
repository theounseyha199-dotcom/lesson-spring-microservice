package kh.edu.istad.platform.business.restapi;

import kh.edu.istad.platform.business.application.CreateBusinessCommand;
import kh.edu.istad.platform.business.application.CreateBusinessUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {
    private final CreateBusinessUseCase createBusiness;
    private final kh.edu.istad.platform.business.application.BusinessRepository repository;
    public BusinessController(CreateBusinessUseCase createBusiness,
            kh.edu.istad.platform.business.application.BusinessRepository repository) {
        this.createBusiness = createBusiness;
        this.repository = repository;
    }
    @GetMapping("/{id}")
    public Mono<BusinessResponse> find(@PathVariable("id") UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND)))
                .map(b -> new BusinessResponse(b.id(), b.name(), b.email(), b.address(), b.status().name(), b.createdAt()));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BusinessResponse> create(@Valid @RequestBody CreateBusinessRequest request) {
        return createBusiness.execute(new CreateBusinessCommand(request.name(), request.email(),
                request.address())).map(b -> new BusinessResponse(b.id(), b.name(), b.email(),
                b.address(), b.status().name(), b.createdAt()));
    }
    public record BusinessResponse(UUID businessId, String name, String email, String address,
                                   String status, Instant createdAt) {}
}
