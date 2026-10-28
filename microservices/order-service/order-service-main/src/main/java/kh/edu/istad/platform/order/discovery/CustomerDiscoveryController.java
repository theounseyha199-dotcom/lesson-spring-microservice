package kh.edu.istad.platform.order.discovery;

import kh.edu.istad.platform.order.client.adapter.CustomerServiceClientAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping({"/api/discovery/customers", "/customers"})
public class CustomerDiscoveryController {
    private final CustomerServiceClientAdapter client;

    public CustomerDiscoveryController(CustomerServiceClientAdapter client) {
        this.client = client;
    }

    @GetMapping
    public Mono<java.util.List<CustomerServiceClientAdapter.CustomerSummary>> list() {
        return client.findAll().onErrorMap(error -> new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Customer service unavailable", error));
    }

    @GetMapping("/{id}")
    public Mono<CustomerServiceClientAdapter.CustomerSummary> find(@PathVariable("id") UUID id) {
        return client.findById(id)
                .onErrorMap(error -> {
                    if (error instanceof WebClientResponseException.NotFound) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found", error);
                    }
                    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Customer service unavailable", error);
                });
    }
}
