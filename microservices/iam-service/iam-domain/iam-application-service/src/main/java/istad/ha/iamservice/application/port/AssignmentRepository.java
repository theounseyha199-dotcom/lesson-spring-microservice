package istad.ha.iamservice.application.port;

import istad.ha.iamservice.domain.model.*;
import java.util.*;

public interface AssignmentRepository {
    UserRole save(UserRole assignment);
    RolePermission save(RolePermission assignment);
    UserPermission save(UserPermission assignment);
    List<UserRole> findUserRoles(long userId);
    List<RolePermission> findRolePermissions(Set<Long> roleIds);
    List<UserPermission> findUserPermissions(long userId);
}
