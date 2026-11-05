package kh.edu.istad.platform.payment.application;

import kh.edu.istad.platform.payment.domain.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
