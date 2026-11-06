package istad.ha.iamservice.application.port;

import istad.ha.iamservice.domain.model.*;
import java.util.*;

public interface IdentityRepository {
    Identity save(Identity identity);
    Optional<Identity> findByUuid(UUID uuid);
    Optional<Identity> findByProviderAndSubject(Provider provider, String subject);
    List<Identity> findByUserId(long userId);
}
