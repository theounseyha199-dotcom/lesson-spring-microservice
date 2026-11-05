package kh.edu.istad.platform.order.persistence.repository;

import kh.edu.istad.platform.order.persistence.entity.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.UUID;

public interface SpringDataOrderMongoRepository extends MongoRepository<OrderDocument, UUID> {}
