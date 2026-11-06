package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.IdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface IdentityJpaRepository extends JpaRepository<IdentityEntity, Long> {
    Optional<IdentityEntity> findByUuid(UUID uuid);
    List<IdentityEntity> findByUserId(long userId);
    Optional<IdentityEntity> findByProviderAndProviderSubject(istad.ha.iamservice.domain.model.Provider provider, String subject);
}
