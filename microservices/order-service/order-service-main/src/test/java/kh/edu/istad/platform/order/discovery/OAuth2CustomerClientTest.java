package kh.edu.istad.platform.order.discovery;

import com.sun.net.httpserver.HttpServer;
import kh.edu.istad.platform.order.client.config.ClientConfig;
import kh.edu.istad.platform.order.client.config.OAuth2ClientConfig;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import kh.edu.istad.platform.order.client.httpclient.PaymentClient;
import kh.edu.istad.platform.order.client.dto.PaymentRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("oauth2")
@SpringBootTest(classes = OAuth2CustomerClientTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.enabled=false", "spring.cloud.consul.config.enabled=false",
                "spring.liquibase.enabled=false"})
class OAuth2CustomerClientTest {
    static final AtomicInteger TOKEN_REQUESTS = new AtomicInteger();
    static final UUID ID = UUID.randomUUID();
    static volatile String tokenAuthorization;
    static volatile String tokenBody;
    static volatile String resourceAuthorization;
    static volatile String paymentBody;
    static volatile String paymentMethod;
    static final HttpServer SERVER = server();

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({ClientConfig.class, OAuth2ClientConfig.class})
    static class TestApplication { }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.customer-service.base-url", OAuth2CustomerClientTest::baseUrl);
        registry.add("spring.http.serviceclient.payment.base-url", OAuth2CustomerClientTest::baseUrl);
        registry.add("spring.security.oauth2.client.registration.payment-client.client-id", () -> "payment-test-client");
        registry.add("spring.security.oauth2.client.registration.payment-client.client-secret", () -> "payment-test-secret");
        registry.add("spring.security.oauth2.client.registration.payment-client.scope", () -> "payment.write");
        registry.add("spring.security.oauth2.client.provider.auth-server.token-uri", () -> baseUrl() + "/oauth/token");
        registry.add("spring.security.oauth2.client.registration.order-service.client-id", () -> "test-client");
        registry.add("spring.security.oauth2.client.registration.order-service.client-secret", () -> "test-secret");
        registry.add("spring.security.oauth2.client.registration.order-service.scope", () -> "customers.read");
    }

    @Autowired CustomerHttpClient client;
    @Autowired PaymentClient paymentClient;

    @Test
    void obtainsServiceTokenAndReusesItForHttpInterfaceCalls() {
        assertThat(client.getCustomerById(ID).block(Duration.ofSeconds(10)).getCustomerId()).isEqualTo(ID);
        assertThat(resourceAuthorization).isEqualTo("Bearer service-token");
        assertThat(client.getCustomerById(ID).block(Duration.ofSeconds(10)).getCustomerId()).isEqualTo(ID);
        assertThat(TOKEN_REQUESTS.get()).isEqualTo(1);
        assertThat(tokenAuthorization).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("test-client:test-secret".getBytes(StandardCharsets.UTF_8)));
        assertThat(tokenBody).contains("grant_type=client_credentials", "scope=customers.read");

        var request = new PaymentRequest(ID, ID, new BigDecimal("12.50"));
        var payment = paymentClient.createPayment(request);
        assertThat(payment.orderId()).isEqualTo(ID);
        assertThat(payment.status()).isEqualTo("PENDING");
        assertThat(paymentMethod).isEqualTo("POST");
        assertThat(paymentBody).contains(ID.toString(), "12.50");
        assertThat(resourceAuthorization).isEqualTo("Bearer service-token");
        assertThat(tokenAuthorization).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("payment-test-client:payment-test-secret".getBytes(StandardCharsets.UTF_8)));
        assertThat(tokenBody).contains("scope=payment.write");
        paymentClient.createPayment(request);
        assertThat(TOKEN_REQUESTS.get()).isEqualTo(2);
    }

    private static String baseUrl() { return "http://localhost:" + SERVER.getAddress().getPort(); }

    private static HttpServer server() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String body;
                if (exchange.getRequestURI().getPath().equals("/oauth/token")) {
                    TOKEN_REQUESTS.incrementAndGet();
                    tokenAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                    tokenBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    body = "{\"access_token\":\"service-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
                } else if (exchange.getRequestURI().getPath().equals("/api/payments")) {
                    resourceAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                    paymentMethod = exchange.getRequestMethod();
                    paymentBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    body = "{\"orderId\":\"" + ID + "\",\"status\":\"PENDING\"}";
                } else {
                    resourceAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                    body = "{\"customerId\":\"" + ID + "\"}";
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            });
            server.start();
            return server;
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    @AfterAll static void stop() { SERVER.stop(0); }
}
