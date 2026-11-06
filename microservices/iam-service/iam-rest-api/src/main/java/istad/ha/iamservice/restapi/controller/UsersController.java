package istad.ha.iamservice.restapi.controller;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.query.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.restapi.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final EnableUserUseCase enableUserUseCase;
    private final DisableUserUseCase disableUserUseCase;
    private final LockUserUseCase lockUserUseCase;
    private final UnlockUserUseCase unlockUserUseCase;
    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final GetUserAuthoritiesUseCase getUserAuthoritiesUseCase;
    private final AssignRolesToUserUseCase assignRolesToUserUseCase;
    private final GrantPermissionsToUserUseCase grantPermissionsToUserUseCase;
    private final RevokeRoleFromUserUseCase revokeRoleFromUserUseCase;
    private final RevokePermissionFromUserUseCase revokePermissionFromUserUseCase;
    private final ListUserIdentitiesUseCase listUserIdentitiesUseCase;
    private final UnlinkIdentityUseCase unlinkIdentityUseCase;
    public UsersController(
            CreateUserUseCase createUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserUseCase getUserUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            DeleteUserUseCase deleteUserUseCase,
            EnableUserUseCase enableUserUseCase,
            DisableUserUseCase disableUserUseCase,
            LockUserUseCase lockUserUseCase,
            UnlockUserUseCase unlockUserUseCase,
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            GetUserAuthoritiesUseCase getUserAuthoritiesUseCase,
            AssignRolesToUserUseCase assignRolesToUserUseCase,
            GrantPermissionsToUserUseCase grantPermissionsToUserUseCase,
            RevokeRoleFromUserUseCase revokeRoleFromUserUseCase,
            RevokePermissionFromUserUseCase revokePermissionFromUserUseCase,
            ListUserIdentitiesUseCase listUserIdentitiesUseCase,
            UnlinkIdentityUseCase unlinkIdentityUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserUseCase = getUserUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.enableUserUseCase = enableUserUseCase;
        this.disableUserUseCase = disableUserUseCase;
        this.lockUserUseCase = lockUserUseCase;
        this.unlockUserUseCase = unlockUserUseCase;
        this.resetUserPasswordUseCase = resetUserPasswordUseCase;
        this.getUserAuthoritiesUseCase = getUserAuthoritiesUseCase;
        this.assignRolesToUserUseCase = assignRolesToUserUseCase;
        this.grantPermissionsToUserUseCase = grantPermissionsToUserUseCase;
        this.revokeRoleFromUserUseCase = revokeRoleFromUserUseCase;
        this.revokePermissionFromUserUseCase = revokePermissionFromUserUseCase;
        this.listUserIdentitiesUseCase = listUserIdentitiesUseCase;
        this.unlinkIdentityUseCase = unlinkIdentityUseCase;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.User create(@Valid @RequestBody Requests.CreateUser request) {
        return Responses.user(createUserUseCase.execute(request.command()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PageResult<UserSummary> list(@RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="20") int size,
            @RequestParam(name="sort", defaultValue="createdAt,desc") String sort,
            @RequestParam(name="search", required=false) String search,
            @RequestParam(name="enabled", required=false) Boolean enabled,
            @RequestParam(name="locked", required=false) Boolean locked) {
        return listUsersUseCase.execute(new UserFilterQuery(page, size, sort, search, enabled, locked));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('user:read')")
    public Responses.User get(@PathVariable("uuid") UUID uuid) {
        return Responses.user(getUserUseCase.execute(uuid));
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('user:update')")
    public Responses.User update(@PathVariable("uuid") UUID uuid, @Valid @RequestBody ProfileRequest request) {
        return Responses.user(updateUserProfileUseCase.execute(request.command(uuid)));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority('user:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("uuid") UUID uuid) {
        deleteUserUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/enable")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable("uuid") UUID uuid) {
        enableUserUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/disable")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable("uuid") UUID uuid) {
        disableUserUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/lock")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lock(@PathVariable("uuid") UUID uuid) {
        lockUserUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/unlock")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlock(@PathVariable("uuid") UUID uuid) {
        unlockUserUseCase.execute(uuid);
    }

    @PutMapping("/{uuid}/password")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable("uuid") UUID uuid, @Valid @RequestBody Requests.ResetPassword request) {
        resetUserPasswordUseCase.execute(request.command(uuid));
    }

    @GetMapping("/{uuid}/authorities")
    @PreAuthorize("hasAuthority('user:read')")
    public Responses.Authorities authorities(@PathVariable("uuid") UUID uuid) {
        return Responses.authorities(getUserAuthoritiesUseCase.execute(uuid));
    }

    @PostMapping("/{uuid}/roles")
    @PreAuthorize("hasAuthority('user:assign')")
    public List<RoleAssignmentResult> assignRoles(@PathVariable("uuid") UUID uuid, @Valid @RequestBody Requests.AssignRoles request) {
        return assignRolesToUserUseCase.execute(new AssignRolesToUserCommand(uuid, request.roleUuids(), request.expiresAt(), request.reason()));
    }

    @PostMapping("/{uuid}/permissions")
    @PreAuthorize("hasAuthority('user:assign')")
    public List<PermissionGrantResult> grantPermissions(@PathVariable("uuid") UUID uuid, @Valid @RequestBody Requests.GrantPermissions request) {
        return grantPermissionsToUserUseCase.execute(new GrantPermissionsToUserCommand(uuid, request.permissionUuids(), request.effect(), request.expiresAt(), request.reason()));
    }

    @DeleteMapping("/{uuid}/roles/{roleUuid}")
    @PreAuthorize("hasAuthority('user:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeRoles(@PathVariable("uuid") UUID uuid, @PathVariable("roleUuid") UUID roleUuid, @Valid @RequestBody Requests.Revoke request) {
        revokeRoleFromUserUseCase.execute(uuid, roleUuid, request.reason());
    }

    @DeleteMapping("/{uuid}/permissions/{permissionUuid}")
    @PreAuthorize("hasAuthority('user:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermissions(@PathVariable("uuid") UUID uuid,
            @PathVariable("permissionUuid") UUID permissionUuid,
            @Valid @RequestBody Requests.Revoke request) {
        revokePermissionFromUserUseCase.execute(uuid, permissionUuid, request.reason());
    }

    @GetMapping("/{uuid}/identities")
    @PreAuthorize("hasAuthority('user:read')")
    public List<Responses.Identity> identities(@PathVariable("uuid") UUID uuid) {
        return listUserIdentitiesUseCase.execute(uuid).stream().map(Responses::identity).toList();
    }

    @DeleteMapping("/{uuid}/identities/{identityUuid}")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable("uuid") UUID uuid, @PathVariable("identityUuid") UUID identityUuid) {
        unlinkIdentityUseCase.execute(uuid, identityUuid);
    }

}
