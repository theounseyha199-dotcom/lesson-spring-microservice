package istad.ha.iamservice.domain.rule;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import istad.ha.iamservice.domain.model.*;

public final class AuthorityResolver {
    private AuthorityResolver() { }
    public static UserAuthorities resolve(User user, Collection<Role> roles, Collection<Permission> permissions,
            Collection<UserRole> userRoles, Collection<RolePermission> rolePermissions,
            Collection<UserPermission> userPermissions, Instant now) {
        Checks.required(user, "user"); Checks.required(now, "now");
        Checks.required(roles, "roles"); Checks.required(permissions, "permissions");
        Checks.required(userRoles, "userRoles"); Checks.required(rolePermissions, "rolePermissions");
        Checks.required(userPermissions, "userPermissions");
        long userId = Checks.reference(user.id(), "userId");
        if (!user.active()) return new UserAuthorities(java.util.Set.of(), java.util.Set.of());
        var roleMap = new HashMap<Long, Role>();
        roles.stream().filter(Role::assignable).forEach(role -> roleMap.put(Checks.reference(role.id(), "roleId"), role));
        var permissionMap = new HashMap<Long, Permission>();
        permissions.stream().filter(Permission::assignable).forEach(permission ->
                permissionMap.put(Checks.reference(permission.id(), "permissionId"), permission));
        var roleIds = new HashSet<Long>(); var roleNames = new HashSet<String>();
        userRoles.stream().filter(a -> a.userId() == userId && a.metadata().activeAt(now)
                && roleMap.containsKey(a.roleId())).forEach(a -> {
                    roleIds.add(a.roleId()); roleNames.add(roleMap.get(a.roleId()).name());
                });
        var grants = new HashSet<String>(); var denials = new HashSet<String>();
        rolePermissions.stream().filter(a -> roleIds.contains(a.roleId()) && a.metadata().activeAt(now)
                && permissionMap.containsKey(a.permissionId())).forEach(a -> grants.add(permissionMap.get(a.permissionId()).name()));
        userPermissions.stream().filter(a -> a.userId() == userId && a.metadata().activeAt(now)
                && permissionMap.containsKey(a.permissionId())).forEach(a -> {
                    var target = a.effect() == PermissionEffect.DENY ? denials : grants;
                    target.add(permissionMap.get(a.permissionId()).name());
                });
        grants.removeAll(denials);
        return new UserAuthorities(roleNames, grants);
    }
}
