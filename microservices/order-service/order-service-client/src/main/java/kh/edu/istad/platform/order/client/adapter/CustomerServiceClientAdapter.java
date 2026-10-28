package kh.edu.istad.platform.order.client.adapter;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
public class CustomerServiceClientAdapter {
    private final WebClient client;

    public CustomerServiceClientAdapter(@LoadBalanced WebClient.Builder builder) {
        this.client = builder.clone().baseUrl("http://customer-service").build();
    }

    public Mono<java.util.List<CustomerSummary>> findAll() {
        return client.get().uri("/api/customers")
                .retrieve().bodyToFlux(CustomerSummary.class).collectList()
                .timeout(Duration.ofSeconds(3));
    }

    public Mono<CustomerSummary> findById(UUID id) {
        return client.get().uri("/api/customers/{id}", id)
                .retrieve().bodyToMono(CustomerSummary.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty customer response")))
                .timeout(Duration.ofSeconds(3));
    }

    public record CustomerSummary(UUID customerId, String username, String familyName,
                                  String givenName, String email, String phoneNumber,
                                  String customerStatus, String customerSegmentType) {}
}
