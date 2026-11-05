package kh.edu.istad.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestApplication.class);

    @Test
    void discoversConfigurationWithoutExplicitImport() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OpenAPI.class);
            assertThat(context.getBean(OpenAPI.class).getInfo().getTitle())
                    .isEqualTo("Seyha Order Service API");
        });
    }

    @Test
    void backsOffWhenApplicationProvidesOpenApi() {
        OpenAPI custom = new OpenAPI();
        runner.withBean("customOpenApi", OpenAPI.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OpenAPI.class);
            assertThat(context.getBean(OpenAPI.class)).isSameAs(custom);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
