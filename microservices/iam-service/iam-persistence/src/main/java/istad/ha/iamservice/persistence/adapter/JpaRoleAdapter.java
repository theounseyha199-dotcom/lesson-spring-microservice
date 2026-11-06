package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.application.port.RoleRepository;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.RoleEntity;
import istad.ha.iamservice.persistence.repository.RoleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class JpaRoleAdapter implements RoleRepository {
    private final RoleJpaRepository repository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public JpaRoleAdapter(RoleJpaRepository repository) { this.repository = repository; }
    @Override public istad.ha.iamservice.application.result.PageResult<Role> search(istad.ha.iamservice.application.query.RoleFilterQuery query) {
        org.springframework.data.jpa.domain.Specification<RoleEntity> specification = (root, criteria, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.isFalse(root.get("deleted")));
            SearchSupport.text(filters, root, cb, query.search(), "name", "description");
            if (query.active() != null) filters.add(cb.equal(root.get("active"), query.active()));
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var page = repository.findAll(specification, SearchSupport.page(query.page(), query.size(), query.sort()));
        return istad.ha.iamservice.application.result.PageResult.of(page.getContent().stream().map(DomainMapper::role).toList(),
                query.page(), query.size(), page.getTotalElements());
    }
    @Override @Transactional public Role save(Role value) {
        var entity = value.id() == null ? new RoleEntity() : repository.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Role not found"));
        if (entity.getId() != null && entity.isSystem() && !DomainMapper.role(entity).equals(value)) {
            throw new istad.ha.iamservice.domain.exception.DomainException("system_protected", "System record cannot be changed");
        }
        DomainMapper.apply(value, entity);
        repository.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.role(entity);
    }
    @Override public Optional<Role> findByUuid(UUID uuid) { return repository.findByUuid(uuid).filter(e -> !e.isDeleted()).map(DomainMapper::role); }
    @Override public Optional<Role> findById(long id) { return repository.findById(id).filter(e -> !e.isDeleted()).map(DomainMapper::role); }
    @Override public List<Role> findByIds(Set<Long> ids) { return repository.findAllById(ids).stream().filter(e -> !e.isDeleted()).map(DomainMapper::role).toList(); }
    @Override public List<Role> findAll() { return repository.findAll().stream().filter(e -> !e.isDeleted()).map(DomainMapper::role).toList(); }
    @Override @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Role> findByUuidForUpdate(UUID uuid) {
        return repository.lockByUuid(uuid).filter(e -> !e.isDeleted()).map(e -> {
            entityManager.refresh(e, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); return DomainMapper.role(e);
        });
    }
    @Override public Optional<Role> findByName(String name) { return repository.findByName(name).map(DomainMapper::role); }
}
