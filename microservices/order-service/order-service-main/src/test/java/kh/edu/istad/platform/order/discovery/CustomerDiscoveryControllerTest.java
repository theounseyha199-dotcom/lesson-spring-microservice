package kh.edu.istad.platform.order.discovery;

import com.sun.net.httpserver.HttpServer;
import kh.edu.istad.platform.order.client.adapter.CustomerServiceClientAdapter;
import kh.edu.istad.platform.order.client.config.ClientConfig;
import kh.edu.istad.platform.order.client.dto.PostCustomer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CustomerDiscoveryControllerTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.liquibase.enabled=false", "spring.cloud.consul.config.enabled=false",
                "spring.cloud.consul.enabled=false",
                "spring.http.serviceclient.customer-service.base-url=lb://customer-service"})
class CustomerDiscoveryControllerTest {
    static final UUID ID = UUID.randomUUID();
    static final HttpServer SERVER = server();
    static volatile String userAgent;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({DiscoveryClientConfiguration.class, ClientConfig.class,
            CustomerServiceClientAdapter.class, CustomerDiscoveryController.class})
    static class TestApplication { }

    @DynamicPropertySource
    static void instances(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.customer-service[0].uri",
                () -> "http://localhost:" + SERVER.getAddress().getPort());
    }

    @Autowired CustomerDiscoveryController controller;

    @Test
    void discoversCustomerAndAppliesCustomHeaders() {
        assertThat(controller.find(ID).block(Duration.ofSeconds(5)).getCustomerId()).isEqualTo(ID);
        assertThat(userAgent).isEqualTo("Web Client");
    }

    @Test
    void preservesCreatedCustomerId() {
        var request = new PostCustomer("user", "Family", "Given", "user@example.com", "012345678", null, null);
        assertThat(controller.create(request).block(Duration.ofSeconds(5)).getCustomerId()).isEqualTo(ID);
    }

    @Test
    void preservesNotFoundStatus() {
        assertThatThrownBy(() -> controller.find(new UUID(0, 0)).block(Duration.ofSeconds(5)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static HttpServer server() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/customers", exchange -> {
                userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
                exchange.getRequestBody().readAllBytes();
                boolean missing = exchange.getRequestURI().getPath().endsWith(new UUID(0, 0).toString());
                byte[] body = (missing ? "{}" : "{\"customerId\":\"" + ID + "\",\"username\":\"user\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(missing ? 404 : exchange.getRequestMethod().equals("POST") ? 201 : 200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @AfterAll static void stop() { SERVER.stop(0); }
}
