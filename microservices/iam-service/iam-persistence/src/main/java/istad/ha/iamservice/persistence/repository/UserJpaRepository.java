package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByUuid(UUID uuid);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);
    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserEntity u where u.uuid = :uuid")
    Optional<UserEntity> lockByUuid(@org.springframework.data.repository.query.Param("uuid") UUID uuid);
}
