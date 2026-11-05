package kh.edu.istad.common.webflux.exception;

import kh.edu.istad.common.domain.Exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

class DomainExceptionHandlerTest {
    @Test
    void returnsErrorResponseForDomainException() {
        WebTestClient.bindToController(new FailingController())
                .controllerAdvice(new DomainExceptionHandler())
                .build().get().uri("/failure").exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Test failure")
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.path").isEqualTo("/failure");
    }

    @Test
    void preservesHttpStatusExceptions() {
        WebTestClient.bindToController(new FailingController())
                .controllerAdvice(new DomainExceptionHandler())
                .build().get().uri("/missing").exchange()
                .expectStatus().isNotFound();
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        String fail() {
            throw new DomainException("Test failure");
        }

        @GetMapping("/missing")
        String missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
