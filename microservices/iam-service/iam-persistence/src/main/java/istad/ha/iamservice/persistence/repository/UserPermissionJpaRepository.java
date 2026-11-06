package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.UserPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserPermissionJpaRepository extends JpaRepository<UserPermissionEntity, Long> {
    List<UserPermissionEntity> findByUserId(long userId);
}
