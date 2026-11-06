package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.application.port.UserRepository;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.UserEntity;
import istad.ha.iamservice.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class JpaUserAdapter implements UserRepository {
    private final UserJpaRepository repository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public JpaUserAdapter(UserJpaRepository repository) { this.repository = repository; }
    @Override public istad.ha.iamservice.application.result.PageResult<User> search(istad.ha.iamservice.application.query.UserFilterQuery query) {
        org.springframework.data.jpa.domain.Specification<UserEntity> specification = (root, criteria, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.isFalse(root.get("deleted")));
            SearchSupport.text(filters, root, cb, query.search(), "username", "email", "givenName", "familyName");
            if (query.enabled() != null) filters.add(cb.equal(root.get("enabled"), query.enabled()));
            if (query.locked() != null) filters.add(cb.equal(root.get("accountNonLocked"), !query.locked()));
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var page = repository.findAll(specification, SearchSupport.page(query.page(), query.size(), query.sort()));
        return istad.ha.iamservice.application.result.PageResult.of(page.getContent().stream().map(DomainMapper::user).toList(),
                query.page(), query.size(), page.getTotalElements());
    }
    @Override @Transactional public User save(User value) {
        var entity = value.id() == null ? new UserEntity() : repository.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "User not found"));
        DomainMapper.apply(value, entity);
        repository.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.user(entity);
    }
    @Override public Optional<User> findByUuid(UUID uuid) { return repository.findByUuid(uuid).filter(e -> !e.isDeleted()).map(DomainMapper::user); }
    @Override public Optional<User> findById(long id) { return repository.findById(id).filter(e -> !e.isDeleted()).map(DomainMapper::user); }
    @Override public List<User> findByIds(Set<Long> ids) { return repository.findAllById(ids).stream().filter(e -> !e.isDeleted()).map(DomainMapper::user).toList(); }
    @Override public List<User> findAll() { return repository.findAll().stream().filter(e -> !e.isDeleted()).map(DomainMapper::user).toList(); }
    @Override public boolean emailExists(String email) { return repository.existsByEmailIgnoreCase(email); }
    @Override public boolean usernameExists(String username) { return repository.existsByUsername(username); }
    @Override public Optional<User> findByUsernameOrEmail(String value) { return repository.findByUsernameAndDeletedFalse(value.strip())
            .or(() -> repository.findByEmailIgnoreCaseAndDeletedFalse(value.strip())).map(DomainMapper::user); }
    @Override @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY) public Optional<User> findByUuidForUpdate(UUID uuid) { return repository.lockByUuid(uuid).filter(e -> !e.isDeleted()).map(e -> { entityManager.refresh(e, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); return DomainMapper.user(e); }); }
}
