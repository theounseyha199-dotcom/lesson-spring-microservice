package kh.edu.istad.platform.order.discovery;

import com.sun.net.httpserver.HttpServer;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = BusinessDiscoveryTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.enabled=false", "spring.cloud.loadbalancer.cache.enabled=false"})
class BusinessDiscoveryTest {
    static final HttpServer FIRST = instance("first");
    static final HttpServer SECOND = instance("second");

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({DiscoveryClientConfiguration.class, BusinessDiscoveryController.class})
    static class TestApplication { }

    @DynamicPropertySource
    static void instances(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.business-service[0].uri",
                () -> "http://localhost:" + FIRST.getAddress().getPort());
        registry.add("spring.cloud.discovery.client.simple.instances.business-service[1].uri",
                () -> "http://localhost:" + SECOND.getAddress().getPort());
    }

    @Autowired
    BusinessDiscoveryController controller;

    @Test
    void distributesRequestsAndReturnsUnavailableWhenInstancesFail() {
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            selected.add((String) controller.businessInstance().block(Duration.ofSeconds(5)).get("instance"));
        }
        assertThat(selected).containsExactlyInAnyOrder("first", "second");

        stopInstances();
        assertThatThrownBy(() -> controller.businessInstance().block(Duration.ofSeconds(5)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private static HttpServer instance(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/businesses/instance", exchange -> {
                byte[] body = ("{\"instance\":\"" + name + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    @AfterAll
    static void stopInstances() {
        FIRST.stop(0);
        SECOND.stop(0);
    }
}
