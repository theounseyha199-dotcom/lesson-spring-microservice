package istad.ha.iamservice.infrastructure.config;

import istad.ha.iamservice.infrastructure.security.IamUserPrincipal;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/** The persistence mapper permits the application principal and its numeric OIDC timestamp in addition to Spring's built-in types. */
public final class JdbcAuthorizationPersistence {
    private JdbcAuthorizationPersistence() {}
    public static JdbcOAuth2AuthorizationService create(JdbcOperations jdbc, RegisteredClientRepository clients) {
        var types = BasicPolymorphicTypeValidator.builder().allowIfSubType(IamUserPrincipal.class).allowIfSubType(Long.class);
        var mapper = JsonMapper.builder().addModules(SecurityJacksonModules.getModules(
                JdbcAuthorizationPersistence.class.getClassLoader(), types)).build();
        var service = new JdbcOAuth2AuthorizationService(jdbc, clients);
        service.setAuthorizationRowMapper(new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(clients, mapper));
        service.setAuthorizationParametersMapper(new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(mapper));
        return service;
    }
}
