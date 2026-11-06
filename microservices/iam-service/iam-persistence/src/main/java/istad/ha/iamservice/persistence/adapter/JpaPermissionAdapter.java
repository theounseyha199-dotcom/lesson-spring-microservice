package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.application.port.PermissionRepository;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.PermissionEntity;
import istad.ha.iamservice.persistence.repository.PermissionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class JpaPermissionAdapter implements PermissionRepository {
    private final PermissionJpaRepository repository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public JpaPermissionAdapter(PermissionJpaRepository repository) { this.repository = repository; }
    @Override public istad.ha.iamservice.application.result.PageResult<Permission> search(istad.ha.iamservice.application.query.PermissionFilterQuery query) {
        org.springframework.data.jpa.domain.Specification<PermissionEntity> specification = (root, criteria, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            filters.add(cb.isFalse(root.get("deleted")));
            SearchSupport.text(filters, root, cb, query.search(), "name", "description");
            if (query.active() != null) filters.add(cb.equal(root.get("active"), query.active()));
            if (query.resource() != null) filters.add(cb.equal(root.get("resource"), query.resource()));
            if (query.action() != null) filters.add(cb.equal(root.get("action"), query.action()));
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var page = repository.findAll(specification, SearchSupport.page(query.page(), query.size(), query.sort()));
        return istad.ha.iamservice.application.result.PageResult.of(page.getContent().stream().map(DomainMapper::permission).toList(),
                query.page(), query.size(), page.getTotalElements());
    }
    @Override @Transactional public Permission save(Permission value) {
        var entity = value.id() == null ? new PermissionEntity() : repository.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Permission not found"));
        if (entity.getId() != null && entity.isSystem() && !DomainMapper.permission(entity).equals(value)) {
            throw new istad.ha.iamservice.domain.exception.DomainException("system_protected", "System record cannot be changed");
        }
        DomainMapper.apply(value, entity);
        repository.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.permission(entity);
    }
    @Override public Optional<Permission> findByUuid(UUID uuid) { return repository.findByUuid(uuid).filter(e -> !e.isDeleted()).map(DomainMapper::permission); }
    @Override public Optional<Permission> findById(long id) { return repository.findById(id).filter(e -> !e.isDeleted()).map(DomainMapper::permission); }
    @Override public List<Permission> findByIds(Set<Long> ids) { return repository.findAllById(ids).stream().filter(e -> !e.isDeleted()).map(DomainMapper::permission).toList(); }
    @Override public List<Permission> findAll() { return repository.findAll().stream().filter(e -> !e.isDeleted()).map(DomainMapper::permission).toList(); }
    @Override public Optional<Permission> findByName(String name) { return repository.findByName(name).map(DomainMapper::permission); }
}
