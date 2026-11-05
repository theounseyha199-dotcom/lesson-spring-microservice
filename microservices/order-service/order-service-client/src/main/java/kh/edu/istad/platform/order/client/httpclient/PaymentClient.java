package kh.edu.istad.platform.order.client.httpclient;

import kh.edu.istad.platform.order.client.dto.PaymentRequest;
import kh.edu.istad.platform.order.client.dto.PaymentResponse;
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/payments")
@ClientRegistrationId("payment-client")
public interface PaymentClient {
    @PostExchange
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
}
