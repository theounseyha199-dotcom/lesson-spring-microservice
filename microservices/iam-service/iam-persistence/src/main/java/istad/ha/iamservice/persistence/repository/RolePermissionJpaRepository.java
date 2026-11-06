package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, Long> {
    List<RolePermissionEntity> findByRoleIdIn(Set<Long> roleIds);
}
