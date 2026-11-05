package kh.edu.istad.platform.customer.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {
    @Test
    void customerEndpointsRequireMatchingScopes() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestApplication.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean(FilterChainProxy.class)).build();
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/customers").header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/customers").header("Authorization", "Bearer api.read"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/customers").header("Authorization", "Bearer customer.read"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/customers").header("Authorization", "Bearer customer.read"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/customers").header("Authorization", "Bearer customer.write"))
                    .andExpect(status().isOk());
            mvc.perform(put("/api/customers/123/deactivate").header("Authorization", "Bearer customer.read"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/customers/123/deactivate").header("Authorization", "Bearer customer.write"))
                    .andExpect(status().isOk());
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
                if ("invalid".equals(token)) throw new BadJwtException("Invalid test token");
                return Jwt.withTokenValue(token).header("alg", "RS256")
                        .subject("order-service").claim("scope", token).build();
            };
        }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"/actuator/health", "/api/customers"})
        String get() { return "ok"; }
        @PostMapping("/api/customers")
        String create() { return "ok"; }
        @PutMapping("/api/customers/{id}/deactivate")
        String deactivate() { return "ok"; }
    }
}
