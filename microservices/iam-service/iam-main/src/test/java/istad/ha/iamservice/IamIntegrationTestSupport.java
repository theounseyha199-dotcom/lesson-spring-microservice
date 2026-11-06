package istad.ha.iamservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

@SpringBootTest(classes = IamServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:iam;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "spring.liquibase.change-log=classpath:db/test-changelog.yaml",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "ORDER_CLIENT_SECRET=test-order-secret", "spring.cloud.consul.enabled=false",
        "app.iam.issuer=http://localhost:9000",
        "spring.security.oauth2.client.registration.github.client-id=test-github",
        "spring.security.oauth2.client.registration.github.client-secret=test-secret",
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.security.oauth2.client.registration.telegram.client-id=test-telegram",
        "spring.security.oauth2.client.registration.telegram.client-secret=test-secret",
        "spring.security.oauth2.client.registration.facebook.client-id=test-facebook",
        "spring.security.oauth2.client.registration.facebook.client-secret=test-secret",
        "app.iam.registration-client-secret=test-registration-secret",
        "app.iam.bootstrap-admin-password=test-admin-only", "app.iam.bootstrap-owner-password=test-owner-only",
        "app.iam.password=password", "app.iam.postman-client-secret=test-postman-only",
        "app.iam.client-secret=test-secret"})
@AutoConfigureMockMvc
abstract class IamIntegrationTestSupport {
    @TempDir static Path keys;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.iam.signing-key-file", () -> keys.resolve("signing.json").toString());
        String url = TestDatabaseSettings.postgresUrl();
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", TestDatabaseSettings::username);
            registry.add("spring.datasource.password", TestDatabaseSettings::password);
            registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        }
    }
}
