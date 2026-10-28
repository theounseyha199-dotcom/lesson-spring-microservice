
package kh.edu.istad.platform.order;

import kh.edu.istad.platform.order.config.ServicePropConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/webflux")
@RefreshScope
@RequiredArgsConstructor
public class WebFluxController {
    private final ServicePropConfig servicePropConfig;


    @Value("${service.message}")
    private String serviceMessage;

    @Value("${service.env}")
    private String serviceEnv;

    @Value("${service.version}")
    private String serviceVersion;

    @GetMapping
    public Flux<String> getFlux() {
        return Flux.just(
                        "Hello", "World", "From", "WebFlux"
                )
                .delayElements(Duration.ofSeconds(1))
                .log();
    }

    @Value("${global.version}")
    private String globalVersion;
    @GetMapping("/consul/config")
    public Mono<Map<String, Object>> getConsulConfig() {
        return Mono.just(
                Map.of(
                        "message", servicePropConfig.getMessage(),
                        "env", servicePropConfig.getEnv(),
                        "version", servicePropConfig.getVersion(),
                        "globalVersion", globalVersion,
                        "baseUrl", servicePropConfig.getBaseUrl()
                )
        );
    }

}
