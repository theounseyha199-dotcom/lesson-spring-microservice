package kh.edu.istad.platform.customer.persistence.repository;
import kh.edu.istad.platform.customer.persistence.document.SegmentDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
public interface SegmentMongoRepository extends ReactiveMongoRepository<SegmentDocument, String> {}
