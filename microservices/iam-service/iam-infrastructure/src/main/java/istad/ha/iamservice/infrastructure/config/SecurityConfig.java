package istad.ha.iamservice.infrastructure.config;


import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .oauth2AuthorizationServer((authorizationServer) -> {

                    http.securityMatcher(
                            authorizationServer.getEndpointsMatcher()
                    );

                    // Enable OpenID Connect 1.0
                    authorizationServer.authorizationEndpoint(endpoint -> endpoint.consentPage("/oauth2/consent"));
                    authorizationServer.oidc(oidc -> oidc.clientRegistrationEndpoint(registration -> registration
                            .authenticationProviders(ClientRegistrationPolicy::configure)));
                })
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint(),
                                new org.springframework.security.web.util.matcher.OrRequestMatcher(
                                        org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults().matcher("/connect/register"),
                                        org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults().matcher("/userinfo")))
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }



    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
            org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver resolver,
            istad.ha.iamservice.infrastructure.federation.FederationAuthorizationRequests requests,
            istad.ha.iamservice.infrastructure.federation.FederationLoginHandler federation)
            throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login", "/register", "/federation/complete", "/federation/cancel", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(form -> form.loginPage("/login").permitAll())
                .oauth2Login(oauth2 -> oauth2.loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver)
                                .authorizationRequestRepository(requests))
                        .successHandler(federation).failureHandler(federation).permitAll());

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return JdbcAuthorizationPersistence.create(jdbcTemplate, clients);
    }

    @Bean
    public org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clients);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @org.springframework.beans.factory.annotation.Value("${app.iam.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }



}
