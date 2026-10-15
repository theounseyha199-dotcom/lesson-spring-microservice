package kh.edu.istad.platform.business.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BusinessMongoRepository extends ReactiveMongoRepository<BusinessDocument, String> {}
