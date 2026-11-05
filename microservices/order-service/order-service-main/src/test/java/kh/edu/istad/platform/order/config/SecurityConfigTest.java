package kh.edu.istad.platform.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {
    @Test
    void allowsDemoAndHealthButProtectsOtherEndpoints() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestApplication.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean(FilterChainProxy.class)).build();

            mvc.perform(get("/customers").header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk());
            mvc.perform(post("/customers").header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/customers").header("Authorization", "Bearer customer-token"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/webflux")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mvc.perform(get("/api/webflux/consul/config")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/checkout")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/checkout").header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/checkout").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, Endpoints.class})
    static class TestApplication {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"valid-token".equals(token) && !"customer-token".equals(token)) {
                    throw new BadJwtException("Invalid test token");
                }
                return Jwt.withTokenValue(token).header("alg", "RS256")
                        .subject("postman-client").claim("scope", "customer-token".equals(token) ? "customer.read" : "api.read").build();
            };
        }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"/api/webflux", "/actuator/health", "/api/checkout", "/customers"})
        String publicEndpoint() {
            return "ok";
        }
    }
}
