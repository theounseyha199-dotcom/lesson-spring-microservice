package kh.edu.istad.platform.order.checkout;

import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class CheckoutServices {
    private final WebClient client;
    private final boolean hardcoded;
    private static final Map<String, String> BASELINE = Map.of(
            "customer", "http://localhost:8084", "business", "http://localhost:8082",
            "payment", "http://localhost:8083");

    public CheckoutServices(@LoadBalanced WebClient.Builder builder, Environment environment) {
        hardcoded = environment.acceptsProfiles(Profiles.of("hardcoded-demo"));
        client = (hardcoded ? WebClient.builder() : builder.clone()).build();
    }

    private String address(String service) {
        return hardcoded ? BASELINE.get(service) : "http://" + service + "-service";
    }

    public void requireActive(String service, UUID id) {
        String collection = service.equals("business") ? "businesses" : "customers";
        var state = await(client.get().uri(address(service) + "/api/" + collection + "/" + id)
                .retrieve().bodyToMono(ServiceState.class), service);
        if (!"ACTIVE".equals(state.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, service + " is not active");
        }
    }

    public PaymentResponse registerPayment(OrderResponse order) {
        return await(client.post().uri(address("payment") + "/api/payments")
                .bodyValue(new PaymentRequest(order.orderId(), order.customerId(), order.total()))
                .retrieve().bodyToMono(PaymentResponse.class), "payment");
    }

    private <T> T await(Mono<T> call, String service) {
        return call.switchIfEmpty(Mono.error(new IllegalStateException("Empty service response")))
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(error -> {
                    if (error instanceof WebClientResponseException response && response.getStatusCode().value() == 404) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, service + " record not found");
                    }
                    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, service + " unavailable", error);
                }).block();
    }

    public record ServiceState(String status) {}
    public record PaymentRequest(UUID orderId, UUID customerId, BigDecimal amount) {}
    public record PaymentResponse(UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount, String status) {}
}
