package kh.edu.istad.platform.payment.application;

import kh.edu.istad.platform.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class CreatePaymentUseCase {
    private final PaymentRepository repository;
    public CreatePaymentUseCase(PaymentRepository repository) { this.repository = repository; }
    public Payment execute(CreatePaymentCommand command) {
        if (command == null) throw new IllegalArgumentException("Payment request is required");
        return repository.save(Payment.create(command.orderId(), command.customerId(), command.amount()));
    }
}
