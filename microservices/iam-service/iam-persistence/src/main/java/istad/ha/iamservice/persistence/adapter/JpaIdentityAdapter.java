package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.application.port.IdentityRepository;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.IdentityEntity;
import istad.ha.iamservice.persistence.repository.IdentityJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class JpaIdentityAdapter implements IdentityRepository {
    private final IdentityJpaRepository repository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public JpaIdentityAdapter(IdentityJpaRepository repository) { this.repository = repository; }
    @Override @Transactional public Identity save(Identity value) {
        var entity = value.id() == null ? new IdentityEntity() : repository.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Identity not found"));
        DomainMapper.apply(value, entity);
        repository.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.identity(entity);
    }
    @Override public Optional<Identity> findByUuid(UUID uuid) { return repository.findByUuid(uuid).map(e -> { entityManager.refresh(e); return DomainMapper.identity(e); }); }
    @Override public Optional<Identity> findByProviderAndSubject(Provider provider, String subject) {
        return repository.findByProviderAndProviderSubject(provider, subject).map(DomainMapper::identity);
    }
    @Override public List<Identity> findByUserId(long userId) { return repository.findByUserId(userId).stream().map(DomainMapper::identity).toList(); }
}
