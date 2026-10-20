package kh.edu.istad.platform.customer.persistence.config;

import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuditorAwareConfig implements ReactiveAuditorAware<String> {

    @Override
    public Mono<String> getCurrentAuditor() {
        return Mono.just("SYSTEM");
    }
}
