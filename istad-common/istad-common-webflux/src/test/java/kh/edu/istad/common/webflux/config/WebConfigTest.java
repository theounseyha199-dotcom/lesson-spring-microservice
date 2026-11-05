package kh.edu.istad.common.webflux.config;

import kh.edu.istad.common.webflux.exception.DomainExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {
    private final ReactiveWebApplicationContextRunner runner =
            new ReactiveWebApplicationContextRunner().withUserConfiguration(TestApplication.class);

    @Test
    void discoversConfigurationInReactiveApplication() {
        runner.run(context -> assertThat(context)
                .hasNotFailed().hasSingleBean(DomainExceptionHandler.class));
    }

    @Test
    void backsOffForCustomHandler() {
        DomainExceptionHandler handler = new DomainExceptionHandler();
        runner.withBean("customHandler", DomainExceptionHandler.class, () -> handler)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(DomainExceptionHandler.class);
                    assertThat(context.getBean(DomainExceptionHandler.class)).isSameAs(handler);
                });
    }

    @Test
    void doesNotRegisterOutsideReactiveApplication() {
        new ApplicationContextRunner().withUserConfiguration(TestApplication.class)
                .run(context -> assertThat(context)
                        .hasNotFailed().doesNotHaveBean(DomainExceptionHandler.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
