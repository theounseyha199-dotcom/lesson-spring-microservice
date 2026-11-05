package istad.ha.iamservice.feature.permission;

import istad.ha.iamservice.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByPermission(String permission);
    Optional<Permission> findByPermission(String permission);
}
