package kh.edu.istad.platform.order.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientConfigTest {
    @Test
    void usesDiscoveryAndReadsCustomerContractWithoutMutatingBuilder() {
        UUID id = UUID.randomUUID();
        var uri = new AtomicReference<String>();
        var builder = WebClient.builder().baseUrl("http://original").exchangeFunction(request -> {
            uri.set(request.url().toString());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("[{\"customerId\":\"" + id + "\",\"username\":\"test\","
                            + "\"familyName\":\"Sok\",\"givenName\":\"Dara\",\"customerStatus\":\"ACTIVE\"}]")
                    .build());
        });
        var result = new ClientConfig(builder).register().getAllCustomers()
                .collectList().block(Duration.ofSeconds(3));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().getCustomerId());
        assertEquals("test", result.getFirst().getUsername());
        assertEquals("Sok", result.getFirst().getFamilyName());
        assertEquals("Dara", result.getFirst().getGivenName());
        assertEquals("ACTIVE", result.getFirst().getCustomerStatus());
        assertEquals("http://customer-service/api/customers", uri.get());
        builder.build().get().uri("/probe").retrieve().toBodilessEntity().block(Duration.ofSeconds(3));
        assertEquals("http://original/probe", uri.get());
    }
}
