package istad.ha.iamservice.persistence.adapter;

import istad.ha.iamservice.application.port.AssignmentRepository;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.persistence.entity.*;
import istad.ha.iamservice.persistence.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class JpaAssignmentAdapter implements AssignmentRepository {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final UserRoleJpaRepository userRoles;
    private final RolePermissionJpaRepository rolePermissions;
    private final UserPermissionJpaRepository userPermissions;
    public JpaAssignmentAdapter(UserRoleJpaRepository userRoles, RolePermissionJpaRepository rolePermissions, UserPermissionJpaRepository userPermissions) {
        this.userRoles = userRoles; this.rolePermissions = rolePermissions; this.userPermissions = userPermissions;
    }
    @Override @Transactional public UserRole save(UserRole value) {
        var entity = value.id() == null ? new UserRoleEntity() : userRoles.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Assignment not found"));
        DomainMapper.apply(value, entity);
        userRoles.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.userRole(entity);
    }
    @Override @Transactional public RolePermission save(RolePermission value) {
        var entity = value.id() == null ? new RolePermissionEntity() : rolePermissions.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Assignment not found"));
        DomainMapper.apply(value, entity);
        rolePermissions.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.rolePermission(entity);
    }
    @Override @Transactional public UserPermission save(UserPermission value) {
        var entity = value.id() == null ? new UserPermissionEntity() : userPermissions.findById(value.id())
                .orElseThrow(() -> new istad.ha.iamservice.domain.exception.DomainException("not_found", "Assignment not found"));
        DomainMapper.apply(value, entity);
        userPermissions.saveAndFlush(entity);
        entityManager.refresh(entity);
        return DomainMapper.userPermission(entity);
    }
    @Override public List<UserRole> findUserRoles(long userId) { return userRoles.findByUserId(userId).stream().map(DomainMapper::userRole).toList(); }
    @Override public List<RolePermission> findRolePermissions(Set<Long> roleIds) {
        if (roleIds.isEmpty()) return List.of();
        return rolePermissions.findByRoleIdIn(roleIds).stream().map(DomainMapper::rolePermission).toList();
    }
    @Override public List<UserPermission> findUserPermissions(long userId) { return userPermissions.findByUserId(userId).stream().map(DomainMapper::userPermission).toList(); }
}
