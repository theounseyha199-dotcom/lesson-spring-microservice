package kh.edu.istad.platform.order.restapi.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RequestMapping("/api/webflux")
@RestController
public class WebFluxController {

    @GetMapping
    public Flux<String> getFlux() {
        return Flux.just("Hello", "World", "From", "WebFlux")
                .log()
                .delayElements(Duration.ofSeconds(1));
    }
}
