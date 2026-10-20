package kh.edu.istad.platform.customer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.*;

// Opt-in integration coverage uses a unique database and removes only that test database afterward.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named="RUN_MONGO_INTEGRATION_TESTS", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerApiIntegrationTest {
    private static final String DATABASE = "customer_test_" + UUID.randomUUID().toString().replace("-", "");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String uri = System.getenv().getOrDefault("CUSTOMER_TEST_MONGO_URI",
                "mongodb://mongod:mongod@localhost:2026/{database}?authSource=admin");
        if (!uri.contains("{database}")) throw new IllegalArgumentException("CUSTOMER_TEST_MONGO_URI must contain {database}");
        registry.add("spring.mongodb.uri", () -> uri.replace("{database}", DATABASE));
        registry.add("server.port", () -> "0");
    }
    @LocalServerPort int port;
    @Autowired ReactiveMongoTemplate template;
    @Autowired kh.edu.istad.platform.customer.domain.port.out.CustomerRepository customers;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private HttpResponse<String> request(String method, String path, Document body, int status) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20));
        builder.header("Content-Type", "application/json").method(method, body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body.toJson()));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode(), response.body());
        return response;
    }
    private Document customer(String username, String segmentId) {
        var body = new Document("username", username).append("familyName", "Chan").append("givenName", "Sokha")
                .append("email", username + "@example.com").append("phoneNumber", "+85512345678");
        if (segmentId != null) body.append("segmentId", segmentId);
        return body;
    }
    private Document json(HttpResponse<String> response) { return Document.parse(response.body()); }
    private String query(String filter) { return "?filter=" + URLEncoder.encode(filter, java.nio.charset.StandardCharsets.UTF_8); }
    @AfterAll void cleanup() {
        assertTrue(template.getMongoDatabase().map(db -> db.getName().equals(DATABASE)).block(Duration.ofSeconds(5)));
        template.getMongoDatabase().flatMap(db -> Mono.from(db.drop())).block(Duration.ofSeconds(10));
    }
    @Test void fullCrudFilteringAndStatistics() throws Exception {
        var empty = json(request("GET", "/api/customers/statistics", null, 200));
        assertEquals(0, empty.getInteger("total"));
        String segment = json(request("POST", "/api/customer-segments",
                new Document("name", "VIP").append("description", "Priority customers"), 201)).getString("segmentId");
        String segmentPath = "/api/customer-segments/" + segment;
        request("GET", segmentPath, null, 200);
        request("POST", "/api/customer-segments", new Document("name", "vip"), 409);
        String first = json(request("POST", "/api/customers", customer("sokha", segment), 201)).getString("customerId");
        String second = json(request("POST", "/api/customers", customer("dara", null), 201)).getString("customerId");
        String path = "/api/customers/" + first;
        request("GET", path, null, 200);
        request("POST", "/api/customers", customer("SOKHA", null), 409);
        request("POST", "/api/customers", customer("invalid", null).append("email", "bad"), 400);
        var update = customer("sokha_new", null).append("familyName", "Updated");
        assertEquals("Updated", json(request("PUT", path, update, 200)).getString("familyName"));
        request("PATCH", path + "/status", new Document("status", "INACTIVE"), 200);
        request("PATCH", path + "/status", new Document("status", "DELETED"), 409);
        String filtered = request("GET", "/api/customers" + query("status:eq:INACTIVE"), null, 200).body();
        assertTrue(filtered.contains(first));
        assertFalse(filtered.contains(second));
        assertEquals("[]", request("GET", "/api/customers?page=10&size=1", null, 200).body());
        request("GET", "/api/customers?size=101", null, 400);
        request("GET", "/api/customers" + query("$where:eq:evil"), null, 400);
        request("GET", "/api/customers?sort=unknown", null, 400);
        var stats = json(request("GET", "/api/customers/statistics", null, 200));
        assertEquals(2, stats.getInteger("total"));
        assertEquals(2, json(request("GET", "/statistics", null, 200)).getInteger("total"));
        String inFilter = request("GET", "/api/customers" + query("status:in:ACTIVE,INACTIVE"), null, 200).body();
        assertTrue(inFilter.contains(first));
        assertTrue(inFilter.contains(second));
        assertEquals("[]", request("GET", "/api/customers" + query("username:contains:.*"), null, 200).body());
        assertTrue(request("GET", "/api/customers" + query("status:eq:INACTIVE") + "&filter="
                + URLEncoder.encode("segmentId:eq:" + segment, java.nio.charset.StandardCharsets.UTF_8), null, 200).body().contains(first));
        assertEquals(2, stats.getList("byStatus", Document.class).size());
        assertEquals(2, stats.getList("bySegment", Document.class).size());
        assertEquals(2, stats.getList("byStatusAndSegment", Document.class).size());
        assertEquals(1, json(request("GET", "/api/customers/statistics" + query("segmentId:eq:" + segment), null, 200)).getInteger("total"));
        request("PUT", segmentPath, new Document("name", "VIP Updated").append("description", "Retired")
                .append("status", "INACTIVE"), 200);
        request("PUT", "/api/customers/" + second + "/segment", new Document("segmentId", segment), 409);
        request("PUT", segmentPath, new Document("name", "VIP Updated").append("status", "ACTIVE"), 200);
        request("PUT", "/api/customers/" + second + "/segment", new Document("segmentId", segment), 200);
        assertTrue(request("GET", "/api/customer-segments?search=VIP", null, 200).body().contains(segment));
        request("DELETE", segmentPath, null, 204);
        request("GET", segmentPath, null, 404);
        request("DELETE", segmentPath, null, 404);
        // Existing memberships remain available to historical aggregation after segment retirement.
        assertEquals(2, json(request("GET", "/api/customers/statistics" + query("segmentId:eq:" + segment), null, 200)).getInteger("total"));
        request("PUT", path + "/segment", new Document("segmentId", segment), 404);
        request("DELETE", path + "/segment", null, 200);
        request("DELETE", path, null, 204);
        request("GET", path, null, 404);
        request("PUT", path, update, 404);
        request("DELETE", path, null, 404);
        assertEquals(1, json(request("GET", "/api/customers/statistics", null, 200)).getInteger("total"));
        assertFalse(request("GET", "/api/customers", null, 200).body().contains(first));
        request("POST", "/api/customers", customer("sokha_new", null), 201);
        // Audit fields and version increments must survive the actual MongoDB round trip.
        var stored = template.findById(first, kh.edu.istad.platform.customer.persistence.document.CustomerDocument.class)
                .block(Duration.ofSeconds(5));
        assertNotNull(stored.createdAt());
        assertNotNull(stored.deletedAt());
        assertTrue(stored.version() > 0);
        var customerId = new kh.edu.istad.common.domain.valueobject.CustomerId(UUID.fromString(second));
        var current = customers.findById(customerId).block(Duration.ofSeconds(5));
        var stale = customers.findById(customerId).block(Duration.ofSeconds(5));
        current.updateCustomer("Concurrent", "First");
        customers.save(current).block(Duration.ofSeconds(5));
        stale.updateCustomer("Stale", "Second");
        assertThrows(kh.edu.istad.platform.customer.domain.exception.ConflictException.class,
                () -> customers.save(stale).block(Duration.ofSeconds(5)));
    }
}
