package kh.edu.istad.platform.payment.restapi;

import kh.edu.istad.platform.payment.application.CreatePaymentCommand;
import kh.edu.istad.platform.payment.application.CreatePaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final CreatePaymentUseCase createPayment;
    public PaymentController(CreatePaymentUseCase createPayment) { this.createPayment = createPayment; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        var payment = createPayment.execute(new CreatePaymentCommand(request.orderId(),
                request.customerId(), request.amount()));
        return new PaymentResponse(payment.id(), payment.orderId(), payment.customerId(),
                payment.amount(), payment.status().name(), payment.createdAt());
    }
    public record PaymentResponse(UUID paymentId, UUID orderId, UUID customerId,
                                  BigDecimal amount, String status, Instant createdAt) {}
}
