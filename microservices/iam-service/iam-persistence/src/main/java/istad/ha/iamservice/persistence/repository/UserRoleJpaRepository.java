package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, Long> {
    List<UserRoleEntity> findByUserId(long userId);
}
