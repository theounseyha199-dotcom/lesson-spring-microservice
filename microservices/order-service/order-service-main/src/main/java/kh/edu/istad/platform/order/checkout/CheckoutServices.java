package kh.edu.istad.platform.order.checkout;

import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import kh.edu.istad.platform.order.client.dto.PaymentRequest;
import kh.edu.istad.platform.order.client.dto.PaymentResponse;
import kh.edu.istad.platform.order.client.httpclient.PaymentClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class CheckoutServices {
    private final WebClient client;
    private final PaymentClient paymentClient;
    private final boolean hardcoded;
    private final Map<String, String> addresses;
    private static final Map<String, String> BASELINE = Map.of(
            "customer", "http://localhost:8084", "business", "http://localhost:8082");

    public CheckoutServices(@Qualifier("serviceWebClient") WebClient.Builder builder, Environment environment,
                            PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
        hardcoded = environment.acceptsProfiles(Profiles.of("hardcoded-demo"));
        addresses = Map.of(
                "customer", environment.getProperty("services.customer-url", "http://customer-service"),
                "business", environment.getProperty("services.business-url", "http://business-service"));
        client = (hardcoded ? WebClient.builder() : builder.clone()).build();
    }

    private String address(String service) {
        return hardcoded ? BASELINE.get(service) : addresses.get(service);
    }

    public void requireActive(String service, UUID id) {
        String collection = service.equals("business") ? "businesses" : "customers";
        var state = await(client.get().uri(address(service) + "/api/" + collection + "/" + id)
                .retrieve().bodyToMono(ServiceState.class), service);
        if (!"ACTIVE".equals(service.equals("customer") ? state.customerStatus() : state.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, service + " is not active");
        }
    }

    public PaymentResponse registerPayment(OrderResponse order) {
        try {
            var response = paymentClient.createPayment(
                    new PaymentRequest(order.orderId(), order.customerId(), order.total()));
            if (response == null) {
                throw new IllegalStateException("Empty payment response");
            }
            return response;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "payment record not found", error);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "payment unavailable", error);
        }
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

    public record ServiceState(String status, String customerStatus) {}
}
