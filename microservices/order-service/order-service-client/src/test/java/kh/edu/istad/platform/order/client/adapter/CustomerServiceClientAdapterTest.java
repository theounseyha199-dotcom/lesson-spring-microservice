package kh.edu.istad.platform.order.client.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceClientAdapterTest {
    @Test
    void readsCustomerContractWithoutMutatingSharedBuilder() {
        UUID id = UUID.randomUUID();
        var uri = new AtomicReference<String>();
        var builder = WebClient.builder().baseUrl("http://original").exchangeFunction(request -> {
            uri.set(request.url().toString());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("{\"customerId\":\"" + id + "\",\"username\":\"test\",\"customerStatus\":\"ACTIVE\"}")
                    .build());
        });
        var result = new CustomerServiceClientAdapter(builder).findById(id).block();
        assertEquals(id, result.customerId());
        assertEquals("test", result.username());
        assertEquals("ACTIVE", result.customerStatus());
        assertEquals("http://customer-service/api/customers/" + id, uri.get());
        builder.build().get().uri("/probe").retrieve().toBodilessEntity().block();
        assertEquals("http://original/probe", uri.get());
    }

    @Test
    void readsCustomerCollectionAndEmptyList() {
        for (String body : java.util.List.of("[]", "[{\"username\":\"test\",\"customerStatus\":\"ACTIVE\"}]")) {
            var builder = WebClient.builder().exchangeFunction(request -> {
                assertEquals("/api/customers", request.url().getPath());
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json").body(body).build());
            });
            var result = new CustomerServiceClientAdapter(builder).findAll().block();
            assertEquals(body.equals("[]") ? 0 : 1, result.size());
        }
    }

    @Test
    void preservesNotFoundForControllerMapping() {
        var builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build()));
        assertThrows(WebClientResponseException.NotFound.class,
                () -> new CustomerServiceClientAdapter(builder).findById(UUID.randomUUID()).block());
    }

    @Test
    void rejectsEmptySuccessfulResponses() {
        var builder = WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build()));
        assertThrows(IllegalStateException.class,
                () -> new CustomerServiceClientAdapter(builder).findById(UUID.randomUUID()).block());
    }
}
