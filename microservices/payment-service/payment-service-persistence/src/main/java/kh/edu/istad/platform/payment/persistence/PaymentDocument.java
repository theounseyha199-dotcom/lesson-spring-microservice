package kh.edu.istad.platform.payment.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("payments")
public record PaymentDocument(@Id String id, String orderId, String customerId,
                              BigDecimal amount, String status, CreditEntryDocument creditEntry,
                              List<CreditHistoryDocument> history, Instant createdAt) {
    public record CreditEntryDocument(String id, String customerId, BigDecimal amount, String type) {}
    public record CreditHistoryDocument(String creditEntryId, String action, Instant createdAt) {}
}
