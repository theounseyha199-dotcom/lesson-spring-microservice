package kh.edu.istad.platform.order.client.config;


import kh.edu.istad.platform.order.client.httpclient.BusinessHttpClient;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.UUID;

@RequiredArgsConstructor
@Configuration
public class ClientConfig {
    private final WebClient.Builder webClient;

    @Bean
    public CustomerHttpClient register(){


        return createClient(CustomerHttpClient.class, "http://customer-service");
    }

    @GetExchange
    public BusinessHttpClient get(UUID id) {
        return createClient(BusinessHttpClient.class, "http://business-service");
    }


    private <T> T createClient(Class<T> type, String baseUrl) {
        WebClient webClientInstance = webClient.clone()
                .baseUrl(baseUrl)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClientInstance))
                .build();

        return factory.createClient(type);
    }
}
