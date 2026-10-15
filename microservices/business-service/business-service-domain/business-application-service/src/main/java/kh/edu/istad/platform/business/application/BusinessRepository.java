package kh.edu.istad.platform.business.application;

import kh.edu.istad.platform.business.domain.Business;
import reactor.core.publisher.Mono;

public interface BusinessRepository {
    Mono<Business> save(Business business);
}
