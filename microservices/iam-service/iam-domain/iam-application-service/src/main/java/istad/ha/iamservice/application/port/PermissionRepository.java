package istad.ha.iamservice.application.port;

import istad.ha.iamservice.domain.model.*;
import java.util.*;

public interface PermissionRepository {
    istad.ha.iamservice.application.result.PageResult<Permission> search(istad.ha.iamservice.application.query.PermissionFilterQuery query);
    Permission save(Permission value);
    Optional<Permission> findById(long id);
    Optional<Permission> findByUuid(UUID uuid);
    List<Permission> findByIds(Set<Long> ids);
    List<Permission> findAll();
    Optional<Permission> findByName(String name);
}
