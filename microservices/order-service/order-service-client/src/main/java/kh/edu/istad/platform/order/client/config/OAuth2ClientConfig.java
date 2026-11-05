package kh.edu.istad.platform.order.client.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.support.OAuth2WebClientHttpServiceGroupConfigurer;

import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer;

import java.util.List;

/** Outbound service credentials, independent of the caller's login or web session. */
@Configuration(proxyBeanMethods = false)
@Profile("oauth2")
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2ClientConfig {
    @Bean
    public OAuth2AuthorizedClientManager restServiceAuthorizedClientManager(OAuth2ClientProperties properties) {
        var registrations = new InMemoryClientRegistrationRepository(
                List.copyOf(new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values()));
        var clients = new InMemoryOAuth2AuthorizedClientService(registrations);
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials().build());
        var principal = new UsernamePasswordAuthenticationToken("order-service", "N/A", List.of());
        return request -> manager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(request.getClientRegistrationId())
                .principal(principal)
                .attributes(attributes -> attributes.putAll(request.getAttributes()))
                .build());
    }

    @Bean
    public OAuth2RestClientHttpServiceGroupConfigurer oauth2RestClientConfigurer(
            OAuth2AuthorizedClientManager restServiceAuthorizedClientManager) {
        return OAuth2RestClientHttpServiceGroupConfigurer.from(restServiceAuthorizedClientManager);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager serviceAuthorizedClientManager(
            OAuth2ClientProperties properties) {
        var registrations = new InMemoryReactiveClientRegistrationRepository(
                List.copyOf(new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values()));
        var clients = new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials().build());
        // Cache service tokens under one principal, regardless of the incoming user.
        var principal = new UsernamePasswordAuthenticationToken("order-service", "N/A", List.of());
        return request -> manager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(request.getClientRegistrationId())
                .principal(principal)
                .attributes(attributes -> attributes.putAll(request.getAttributes()))
                .build());
    }

    @Bean
    public OAuth2WebClientHttpServiceGroupConfigurer oauth2HttpServiceGroupConfigurer(
            ReactiveOAuth2AuthorizedClientManager serviceAuthorizedClientManager) {
        return OAuth2WebClientHttpServiceGroupConfigurer.from(serviceAuthorizedClientManager);
    }
}
