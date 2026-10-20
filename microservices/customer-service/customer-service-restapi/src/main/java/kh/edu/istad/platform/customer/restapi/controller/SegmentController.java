package kh.edu.istad.platform.customer.restapi.controller;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import kh.edu.istad.platform.customer.domain.dto.*;
import kh.edu.istad.platform.customer.domain.usecase.SegmentCustomerUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.*;

/** Customer Segment CRUD; request and response DTOs hide persistence-specific version details. */
@RestController
@RequestMapping("/api/customer-segments")
@RequiredArgsConstructor
public class SegmentController {
    private final SegmentCustomerUseCase service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SegmentResponse> create(@Valid @RequestBody CreateSegment request) {
        return service.create(request.name(), request.description()).map(SegmentResponse::from);
    }
    @GetMapping
    public Mono<List<SegmentResponse>> list(@RequestParam org.springframework.util.MultiValueMap<String, String> parameters,
            @RequestParam(name="search", required=false) String search, @RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="20") int size, @RequestParam(name="sort", defaultValue="createdAt") String sort,
            @RequestParam(name="direction", defaultValue="desc") String direction) {
        return service.list(new FilterRequest(parameters.get("filter"), search, page, size, sort, direction)).map(SegmentResponse::from).collectList();
    }
    @GetMapping("/{segmentId}")
    public Mono<SegmentResponse> get(@PathVariable("segmentId") UUID segmentId) { return service.get(segmentId).map(SegmentResponse::from); }
    @PutMapping("/{segmentId}")
    public Mono<SegmentResponse> update(@PathVariable("segmentId") UUID segmentId, @Valid @RequestBody UpdateSegment request) {
        return service.update(segmentId, request.name(), request.description(), request.status()).map(SegmentResponse::from);
    }
    @DeleteMapping("/{segmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable("segmentId") UUID segmentId) { return service.delete(segmentId); }
    public record SegmentResponse(UUID segmentId, String name, String description, CustomerSegment.Status status,
                                  java.time.Instant createdAt, java.time.Instant updatedAt) {
        static SegmentResponse from(CustomerSegment s) {
            return new SegmentResponse(s.segmentId(), s.name(), s.description(), s.status(), s.createdAt(), s.updatedAt());
        }
    }
    public record CreateSegment(@NotBlank @Size(max=100) String name, @Size(max=1000) String description) {}
    public record UpdateSegment(@NotBlank @Size(max=100) String name, @Size(max=1000) String description,
                                @NotNull CustomerSegment.Status status) {}
}
