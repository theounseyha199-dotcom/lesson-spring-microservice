package istad.ha.iamservice.persistence.repository;

import istad.ha.iamservice.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<RoleEntity> {
    Optional<RoleEntity> findByUuid(UUID uuid);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RoleEntity r where r.uuid = :uuid")
    Optional<RoleEntity> lockByUuid(@org.springframework.data.repository.query.Param("uuid") UUID uuid);
    Optional<RoleEntity> findByName(String name);
}
