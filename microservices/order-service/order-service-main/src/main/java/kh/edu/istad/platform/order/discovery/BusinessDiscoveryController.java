package kh.edu.istad.platform.order.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
public class BusinessDiscoveryController {
    private final WebClient businessClient;

    public BusinessDiscoveryController(@Qualifier("serviceWebClient") WebClient.Builder builder,
            @Value("${services.business-url:http://business-service}") String baseUrl) {
        this.businessClient = builder.clone().baseUrl(baseUrl).build();
    }

    @GetMapping("/api/discovery/business")
    public Mono<Map<String, String>> businessInstance() {
        return businessClient.get().uri("/api/businesses/instance")
                .retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, String>>() { })
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(error -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Business service unavailable", error));
    }
}