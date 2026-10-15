package kh.edu.istad.platform.business.persistence;

import kh.edu.istad.platform.business.application.BusinessRepository;
import kh.edu.istad.platform.business.domain.Business;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class BusinessRepositoryAdapter implements BusinessRepository {
    private final BusinessMongoRepository mongo;
    public BusinessRepositoryAdapter(BusinessMongoRepository mongo) { this.mongo = mongo; }
    @Override public Mono<Business> save(Business business) {
        var document = new BusinessDocument(business.id().toString(), business.name(),
                business.email(), business.address(), business.status().name(), business.createdAt());
        return mongo.save(document).thenReturn(business);
    }
}
