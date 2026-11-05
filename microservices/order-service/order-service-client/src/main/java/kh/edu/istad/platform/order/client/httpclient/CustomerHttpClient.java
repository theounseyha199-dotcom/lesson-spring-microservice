package kh.edu.istad.platform.order.client.httpclient;

import kh.edu.istad.platform.order.client.dto.PostCustomer;
import kh.edu.istad.platform.order.client.dto.QueryCustomerResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@ClientRegistrationId("order-service")
@HttpExchange("/api/customers")
public interface CustomerHttpClient {

    @GetExchange
    Flux<QueryCustomerResult> getAllCustomers();

    @GetExchange("/{id}")
    Mono<QueryCustomerResult> getCustomerById(@PathVariable("id") UUID id);

    @PostExchange
    Mono<QueryCustomerResult> createCustomer(@RequestBody PostCustomer customer);
}
