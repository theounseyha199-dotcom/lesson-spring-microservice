package kh.edu.istad.platform.order.discovery;

import kh.edu.istad.platform.order.client.config.WebClientConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(WebClientConfig.class)
public class DiscoveryClientConfiguration {
}
