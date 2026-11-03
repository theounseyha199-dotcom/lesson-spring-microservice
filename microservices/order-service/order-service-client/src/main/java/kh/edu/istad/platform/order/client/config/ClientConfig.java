package kh.edu.istad.platform.order.client.config;

import kh.edu.istad.platform.order.client.exception.DownStreamServiceException;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import kh.edu.istad.platform.order.client.httpclient.PaymentClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "customer-service", types = CustomerHttpClient.class,
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT)
@ImportHttpServices(group = "payment", types = PaymentClient.class,
        clientType = HttpServiceGroup.ClientType.REST_CLIENT)
public class ClientConfig {
    @Bean
    public RestClientHttpServiceGroupConfigurer paymentTransportConfigurer() {
        var http = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return groups -> groups.filterByName("payment")
                .forEachClient((group, builder) -> builder.requestFactory(factory));
    }

    @Bean
    public WebClientHttpServiceGroupConfigurer httpServiceTransportConfigurer() {
        // Do not retry payment writes automatically after a connection failure.
        var http = HttpClient.create().disableRetry(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3));
        return groups -> groups.forEachClient((group, builder) -> builder
                .clientConnector(new ReactorClientHttpConnector(http)));
    }

    @Bean
    public WebClientHttpServiceGroupConfigurer webClientHttpServiceGroupConfigurer() {
        return groups -> groups.filterByName("customer-service")
                .forEachClient((group, builder) -> builder
                        .defaultHeader("User-Agent", "Web Client")
                        .defaultStatusHandler(HttpStatusCode::is5xxServerError,
                                response -> response.createException().map(
                                        error -> new DownStreamServiceException("customer-service", error))));
    }
}
