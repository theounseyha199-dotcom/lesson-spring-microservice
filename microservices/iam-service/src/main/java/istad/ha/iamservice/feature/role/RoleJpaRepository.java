package istad.ha.iamservice.feature.role;

import istad.ha.iamservice.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<Role, Long> {
    boolean existsByRole(String role);
    Optional<Role> findByRole(String role);
}
