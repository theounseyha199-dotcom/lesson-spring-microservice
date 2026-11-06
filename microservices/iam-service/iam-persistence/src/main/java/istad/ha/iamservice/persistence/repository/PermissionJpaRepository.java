package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<PermissionEntity> {
    Optional<PermissionEntity> findByUuid(UUID uuid);
    Optional<PermissionEntity> findByName(String name);
}
