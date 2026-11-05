package kh.edu.istad.platform.customer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTest {
    @Test
    void configuredDecoderChecksSignatureIssuerAudienceAndExpiry() throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        var otherKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new FileSystemResource("src/main/resources/application.yaml"));
            var properties = yaml.getObject();
            String audience = properties.getProperty("spring.security.oauth2.resourceserver.jwt.audiences");
            assertEquals("customer-service", audience);
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
                    .withPropertyValues(
                            "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000",
                            "spring.security.oauth2.resourceserver.jwt.audiences=" + audience,
                            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:"
                                    + server.getAddress().getPort() + "/jwks")
                    .run(context -> {
                        var decoder = context.getBean(JwtDecoder.class);
                        assertEquals("order-service", decoder.decode(token(key, "http://localhost:9000",
                                "customer-service", 300)).getSubject());
                        assertThrows(JwtException.class, () -> decoder.decode(token(key,
                                "http://localhost:9000", "order-service", 300)));
                        assertThrows(JwtException.class, () -> decoder.decode(token(key,
                                "http://wrong-issuer", "customer-service", 300)));
                        assertThrows(JwtException.class, () -> decoder.decode(token(key,
                                "http://localhost:9000", "customer-service", -300)));
                        assertThrows(JwtException.class, () -> decoder.decode(token(otherKey,
                                "http://localhost:9000", "customer-service", 300)));
                    });
        } finally {
            server.stop(0);
        }
    }

    private String token(com.nimbusds.jose.jwk.RSAKey key, String issuer, String audience, long expiresIn)
            throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject("order-service")
                .audience(audience).claim("scope", "customer.read")
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(Instant.now().plusSeconds(expiresIn))).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID("test-key").build(), claims);
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
