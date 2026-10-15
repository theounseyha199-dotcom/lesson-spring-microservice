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
    public BusinessController(CreateBusinessUseCase createBusiness) { this.createBusiness = createBusiness; }
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
