package kh.edu.istad.platform.order.checkout;

import jakarta.validation.Valid;
import kh.edu.istad.platform.order.domain.usecase.CreateOrderUseCase;
import kh.edu.istad.platform.order.restapi.dto.CreateOrderRequest;
import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import kh.edu.istad.platform.order.restapi.mapper.OrderWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Coordinates independent local transactions; payment failure cannot roll back an order. */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutServices services;
    private final CreateOrderUseCase orders;
    private final OrderWebMapper mapper;

    public CheckoutController(CheckoutServices services, CreateOrderUseCase orders, OrderWebMapper mapper) {
        this.services = services;
        this.orders = orders;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CreateOrderRequest request) {
        services.requireActive("customer", request.customerId());
        services.requireActive("business", request.businessId());
        var order = mapper.toResponse(orders.execute(mapper.toCommand(request)));
        try {
            var payment = services.registerPayment(order);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CheckoutResponse(order, payment, "PAYMENT_PENDING"));
        } catch (org.springframework.web.server.ResponseStatusException unavailable) {
            // The remote write may have committed before the response was lost. Never retry blindly.
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new CheckoutResponse(order, null, "PAYMENT_RECONCILIATION_REQUIRED"));
        }
    }

    public record CheckoutResponse(OrderResponse order, CheckoutServices.PaymentResponse payment,
                                   String outcome) {}
}
