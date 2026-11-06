package istad.ha.iamservice.application.port;

import istad.ha.iamservice.domain.model.*;
import java.util.*;

public interface RoleRepository {
    istad.ha.iamservice.application.result.PageResult<Role> search(istad.ha.iamservice.application.query.RoleFilterQuery query);
    Role save(Role value);
    Optional<Role> findByUuidForUpdate(UUID uuid);
    Optional<Role> findById(long id);
    Optional<Role> findByUuid(UUID uuid);
    List<Role> findByIds(Set<Long> ids);
    List<Role> findAll();
    Optional<Role> findByName(String name);
}
