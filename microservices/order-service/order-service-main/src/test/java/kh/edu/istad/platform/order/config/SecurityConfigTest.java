package kh.edu.istad.platform.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
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

            mvc.perform(get("/api/webflux")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mvc.perform(get("/api/webflux/consul/config")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/checkout")).andExpect(status().isUnauthorized());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, Endpoints.class})
    static class TestApplication { }

    @RestController
    static class Endpoints {
        @GetMapping({"/api/webflux", "/actuator/health"})
        String publicEndpoint() {
            return "ok";
        }
    }
}
