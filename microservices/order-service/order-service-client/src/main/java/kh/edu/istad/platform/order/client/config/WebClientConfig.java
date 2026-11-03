package kh.edu.istad.platform.order.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import java.time.Duration;

@Configuration
public class WebClientConfig {
    private WebClient.Builder timedBuilder() {
        var http = HttpClient.create().disableRetry(true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(http));
    }

    @Bean
    @LoadBalanced
    @Qualifier("serviceWebClient")
    public WebClient.Builder webClient() { return timedBuilder(); }

}
