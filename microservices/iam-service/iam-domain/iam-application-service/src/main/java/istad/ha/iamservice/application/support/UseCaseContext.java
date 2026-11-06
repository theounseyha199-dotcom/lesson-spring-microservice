package istad.ha.iamservice.application.support;

import istad.ha.iamservice.application.*;
import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.domain.model.*;
import istad.ha.iamservice.domain.rule.*;
import java.util.*;
import java.time.*;
import java.util.stream.Collectors;

/** Shared dependencies and projections; command orchestration stays in individual use cases. */
public record UseCaseContext(UserRepository users, RoleRepository roles, PermissionRepository permissions,
        IdentityRepository identities, AssignmentRepository assignments, PasswordHasher passwords,
        CurrentActorProvider actors, TransactionRunner transactions, Clock clock) {
    public UseCaseContext {
        Objects.requireNonNull(users); Objects.requireNonNull(roles); Objects.requireNonNull(permissions);
        Objects.requireNonNull(identities); Objects.requireNonNull(assignments); Objects.requireNonNull(passwords);
        Objects.requireNonNull(actors); Objects.requireNonNull(transactions); Objects.requireNonNull(clock);
    }
    public Instant now() { return clock.instant(); }
    public String actor() { return Checks.text(actors.currentActor(), "actor", 100); }
    public static ApplicationException missing(String resource) { return new ApplicationException("not_found", resource + " not found"); }
    public User user(UUID uuid) { Checks.required(uuid, "userUuid"); return users.findByUuid(uuid).orElseThrow(() -> missing("User")); }
    public User lockedUser(UUID uuid) { Checks.required(uuid, "userUuid"); return users.findByUuidForUpdate(uuid).orElseThrow(() -> missing("User")); }
    public Role role(UUID uuid) { Checks.required(uuid, "roleUuid"); return roles.findByUuid(uuid).orElseThrow(() -> missing("Role")); }
    public Role lockedRole(UUID uuid) { Checks.required(uuid, "roleUuid"); return roles.findByUuidForUpdate(uuid).orElseThrow(() -> missing("Role")); }
    public Permission permission(UUID uuid) { Checks.required(uuid, "permissionUuid"); return permissions.findByUuid(uuid).orElseThrow(() -> missing("Permission")); }
    public Role defaultRole() {
        var role = roles.findByName("USER").filter(Role::assignable).orElseThrow(() -> missing("Active default role"));
        return role;
    }
    public void uniqueUser(String username, String email) {
        if (users.usernameExists(username) || users.emailExists(email)) throw new ApplicationException("conflict", "Username or email is already registered");
    }
    public void ids(List<UUID> ids, boolean required) {
        Checks.required(ids, "assignment identifiers");
        Checks.require((!required || !ids.isEmpty()) && ids.size() <= 100 && ids.stream().noneMatch(Objects::isNull)
                && new HashSet<>(ids).size() == ids.size(), "validation", "Provide up to 100 distinct identifiers");
    }
    public UserSummary summary(User user) { return new UserSummary(user.uuid(), user.username(), user.email(), user.name(), user.profile().locale(), user.enabled(), user.audit().createdAt(), user.audit().updatedAt()); }
    public RoleAssignmentResult roleAssignment(UserRole a, Role role) {
        var m = a.metadata(); return new RoleAssignmentResult(a.id(), role.uuid(), role.name(), m.assignedAt(), m.assignedBy(), m.expiresAt(), m.revokedAt(), m.revokedBy(), m.reason(), m.audit().version());
    }
    public PermissionAssignmentResult permissionAssignment(RolePermission a, Permission p) {
        var m = a.metadata(); return new PermissionAssignmentResult(a.id(), p.uuid(), p.name(), m.assignedAt(), m.assignedBy(), m.expiresAt(), m.revokedAt(), m.revokedBy(), m.reason(), m.audit().version());
    }
    public PermissionGrantResult permissionGrant(UserPermission a, Permission p) {
        var m = a.metadata(); return new PermissionGrantResult(a.id(), p.uuid(), p.name(), a.effect(), m.assignedAt(), m.assignedBy(), m.expiresAt(), m.revokedAt(), m.revokedBy(), m.reason(), m.audit().version());
    }
    public PermissionResult permissionResult(Permission p) { return new PermissionResult(p.uuid(), p.name(), p.resource(), p.action(), p.description(), p.system(), p.active(), p.audit().createdAt(), p.audit().updatedAt(), p.audit().version()); }
    public IdentityResult identityResult(Identity i) { return new IdentityResult(i.uuid(), i.provider(), i.email(), i.emailVerified(), i.displayName(), i.username(), i.pictureUrl(), i.linkedAt(), i.lastLoginAt()); }
    public UserAuthoritiesResult authorities(User user) {
        var memberships = assignments.findUserRoles(user.id());
        var roleIds = memberships.stream().map(UserRole::roleId).collect(Collectors.toSet());
        var roleGrants = assignments.findRolePermissions(roleIds);
        var direct = assignments.findUserPermissions(user.id());
        var permissionIds = new HashSet<Long>();
        roleGrants.forEach(a -> permissionIds.add(a.permissionId())); direct.forEach(a -> permissionIds.add(a.permissionId()));
        var roleModels = roles.findByIds(roleIds); var permissionModels = permissions.findByIds(permissionIds);
        var effective = AuthorityResolver.resolve(user, roleModels, permissionModels, memberships, roleGrants, direct, now());
        var roleMap = roleModels.stream().collect(Collectors.toMap(Role::id, x -> x));
        var permissionMap = permissionModels.stream().collect(Collectors.toMap(Permission::id, x -> x));
        var roleResults = memberships.stream().filter(a -> a.metadata().activeAt(now()) && roleMap.containsKey(a.roleId())
                && roleMap.get(a.roleId()).assignable()).map(a -> roleAssignment(a, roleMap.get(a.roleId()))).toList();
        var directResults = direct.stream().filter(a -> a.metadata().activeAt(now()) && permissionMap.containsKey(a.permissionId())
                && permissionMap.get(a.permissionId()).assignable()).map(a -> permissionGrant(a, permissionMap.get(a.permissionId()))).toList();
        return new UserAuthoritiesResult(roleResults, directResults, effective.effectivePermissions(), effective.authorities());
    }
    public AuthUserResult auth(User user) {
        return new AuthUserResult(user.uuid(), user.username(), user.email(), user.emailVerified(), user.profile(), user.passwordHash(),
                authorities(user).authorities(), user.accountNonExpired(), user.accountNonLocked(), user.credentialsNonExpired(), user.active());
    }
    public UserResult userResult(User user) {
        var roles = authorities(user).roles().stream().map(a -> new RoleSummary(a.roleUuid(), a.name(), true)).distinct().toList();
        return new UserResult(user.uuid(), user.username(), user.email(), user.emailVerified(), user.name(), user.profile(), user.enabled(),
                user.accountNonExpired(), user.accountNonLocked(), user.credentialsNonExpired(), user.failedLoginAttempts(),
                user.lastLoginAt(), user.passwordChangedAt(), roles, user.audit().createdAt(), user.audit().updatedAt(), user.audit().version());
    }
    public RoleResult roleResult(Role role) {
        var grants = assignments.findRolePermissions(Set.of(role.id()));
        var ids = grants.stream().map(RolePermission::permissionId).collect(Collectors.toSet());
        var map = permissions.findByIds(ids).stream().collect(Collectors.toMap(Permission::id, x -> x));
        var results = grants.stream().filter(a -> a.metadata().activeAt(now()) && map.containsKey(a.permissionId()))
                .map(a -> permissionAssignment(a, map.get(a.permissionId()))).toList();
        return new RoleResult(role.uuid(), role.name(), role.description(), role.system(), role.active(), results,
                role.audit().createdAt(), role.audit().updatedAt(), role.audit().version());
    }
    public Identity providerSnapshot(User user, Identity existing, istad.ha.iamservice.application.command.FederatedIdentityCommand command, boolean restore) {
        Instant at = now();
        return new Identity(existing == null ? null : existing.id(), existing == null ? UUID.randomUUID() : existing.uuid(), user.id(),
                command.provider(), command.providerSubject(), command.email(), command.emailVerified(), command.displayName(), command.username(),
                command.pictureUrl(), command.attributes(), existing == null ? at : existing.linkedAt(), existing == null ? null : existing.lastLoginAt(),
                restore || existing == null ? SoftDeletion.active() : existing.deletion(), existing == null ? AuditMetadata.create(actor(), at) : existing.audit().touch(actor(), at));
    }
}
