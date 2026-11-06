package istad.ha.iamservice.application.port;

import istad.ha.iamservice.domain.model.*;
import java.util.*;

public interface UserRepository {
    istad.ha.iamservice.application.result.PageResult<User> search(istad.ha.iamservice.application.query.UserFilterQuery query);
    User save(User value);
    boolean emailExists(String email);
    boolean usernameExists(String username);
    Optional<User> findById(long id);
    Optional<User> findByUuid(UUID uuid);
    List<User> findByIds(Set<Long> ids);
    List<User> findAll();
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    Optional<User> findByUuidForUpdate(UUID uuid);
}
