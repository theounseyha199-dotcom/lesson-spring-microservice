package kh.edu.istad.platform.payment.persistence;

import kh.edu.istad.platform.payment.application.PaymentRepository;
import kh.edu.istad.platform.payment.domain.Payment;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentMongoRepository mongo;
    public PaymentRepositoryAdapter(PaymentMongoRepository mongo) { this.mongo = mongo; }
    @Override public Payment save(Payment payment) {
        var entry = payment.creditEntry();
        var entryDoc = new PaymentDocument.CreditEntryDocument(entry.id().toString(),
                entry.customerId().toString(), entry.amount(), entry.type().name());
        var history = payment.history().stream().map(h ->
                new PaymentDocument.CreditHistoryDocument(h.creditEntryId().toString(),
                        h.action(), h.createdAt())).toList();
        mongo.save(new PaymentDocument(payment.id().toString(), payment.orderId().toString(),
                payment.customerId().toString(), payment.amount(), payment.status().name(),
                entryDoc, history, payment.createdAt()));
        return payment;
    }
}
