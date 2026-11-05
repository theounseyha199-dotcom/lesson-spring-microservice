package kh.edu.istad.common.webflux.config;


import kh.edu.istad.common.webflux.exception.DomainExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class WebConfig {
    @Bean
    @ConditionalOnMissingBean
    public DomainExceptionHandler domainExceptionHandler() {
        return new DomainExceptionHandler();
    }
}
