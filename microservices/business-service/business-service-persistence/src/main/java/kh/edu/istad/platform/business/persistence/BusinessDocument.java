package kh.edu.istad.platform.business.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("businesses")
public record BusinessDocument(@Id String id, String name, String email, String address,
                               String status, Instant createdAt) {}
