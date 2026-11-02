package kh.edu.istad.platform.order.client.adapter;

import kh.edu.istad.platform.order.client.dto.QueryCustomerResult;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerServiceClientAdapter {
    private final CustomerHttpClient customerHttpClient;

    public Flux<QueryCustomerResult> getAllCustomers() {
        return customerHttpClient.getAllCustomers().timeout(Duration.ofSeconds(3));
    }

    public Mono<List<QueryCustomerResult>> findAll() {
        return getAllCustomers().collectList().timeout(Duration.ofSeconds(3));
    }

    public Mono<QueryCustomerResult> findById(UUID id) {
        return customerHttpClient.getCustomerById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty customer response")))
                .timeout(Duration.ofSeconds(3));
    }
}
