package kh.edu.istad.platform.order.discovery;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/** Part 1 baseline: the caller is deliberately coupled to this host and port. */
@RestController
@Profile("hardcoded-demo")
public class HardCodedBusinessController {
    private final WebClient businessClient = WebClient.builder()
            .baseUrl("http://localhost:8082")
            .build();

    @GetMapping("/api/hardcoded/business")
    public Mono<Map<String, String>> businessInstance() {
        return businessClient.get().uri("/api/businesses/instance")
                .retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, String>>() { })
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Business service unavailable at hard-coded address http://localhost:8082", error));
    }
}
