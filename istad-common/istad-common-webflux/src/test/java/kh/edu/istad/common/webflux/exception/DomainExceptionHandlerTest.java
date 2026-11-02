package kh.edu.istad.common.webflux.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class DomainExceptionHandlerTest {
    @Test
    void returnsErrorResponseForRuntimeException() {
        WebTestClient.bindToController(new FailingController())
                .controllerAdvice(new DomainExceptionHandler())
                .build().get().uri("/failure").exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Test failure")
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.path").isEqualTo("/failure");
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        String fail() {
            throw new RuntimeException("Test failure");
        }
    }
}
