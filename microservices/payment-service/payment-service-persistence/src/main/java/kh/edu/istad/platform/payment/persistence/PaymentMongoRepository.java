package kh.edu.istad.platform.payment.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentMongoRepository extends MongoRepository<PaymentDocument, String> {}
