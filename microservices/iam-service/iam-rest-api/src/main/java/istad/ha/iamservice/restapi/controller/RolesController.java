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
@RequestMapping("/api/roles")
public class RolesController {
    private final CreateRoleUseCase createRoleUseCase;
    private final ListRolesUseCase listRolesUseCase;
    private final GetRoleUseCase getRoleUseCase;
    private final UpdateRoleUseCase updateRoleUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;
    private final ActivateRoleUseCase activateRoleUseCase;
    private final DeactivateRoleUseCase deactivateRoleUseCase;
    private final AssignPermissionsToRoleUseCase assignPermissionsToRoleUseCase;
    private final RevokePermissionFromRoleUseCase revokePermissionFromRoleUseCase;
    public RolesController(
            CreateRoleUseCase createRoleUseCase,
            ListRolesUseCase listRolesUseCase,
            GetRoleUseCase getRoleUseCase,
            UpdateRoleUseCase updateRoleUseCase,
            DeleteRoleUseCase deleteRoleUseCase,
            ActivateRoleUseCase activateRoleUseCase,
            DeactivateRoleUseCase deactivateRoleUseCase,
            AssignPermissionsToRoleUseCase assignPermissionsToRoleUseCase,
            RevokePermissionFromRoleUseCase revokePermissionFromRoleUseCase) {
        this.createRoleUseCase = createRoleUseCase;
        this.listRolesUseCase = listRolesUseCase;
        this.getRoleUseCase = getRoleUseCase;
        this.updateRoleUseCase = updateRoleUseCase;
        this.deleteRoleUseCase = deleteRoleUseCase;
        this.activateRoleUseCase = activateRoleUseCase;
        this.deactivateRoleUseCase = deactivateRoleUseCase;
        this.assignPermissionsToRoleUseCase = assignPermissionsToRoleUseCase;
        this.revokePermissionFromRoleUseCase = revokePermissionFromRoleUseCase;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResult create(@Valid @RequestBody Requests.CreateRole request) {
        return createRoleUseCase.execute(request.command());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public PageResult<RoleSummary> list(@RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="20") int size,
            @RequestParam(name="sort", defaultValue="createdAt,desc") String sort,
            @RequestParam(name="search", required=false) String search,
            @RequestParam(name="active", required=false) Boolean active) {
        return listRolesUseCase.execute(new RoleFilterQuery(page, size, sort, search, active));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('role:read')")
    public RoleResult get(@PathVariable("uuid") UUID uuid) {
        return getRoleUseCase.execute(uuid);
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('role:update')")
    public RoleResult update(@PathVariable("uuid") UUID uuid, @Valid @RequestBody RolePatchRequest request) {
        return updateRoleUseCase.execute(request.command(uuid));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority('role:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("uuid") UUID uuid) {
        deleteRoleUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/activate")
    @PreAuthorize("hasAuthority('role:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable("uuid") UUID uuid) {
        activateRoleUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/deactivate")
    @PreAuthorize("hasAuthority('role:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("uuid") UUID uuid) {
        deactivateRoleUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/permissions")
    @PreAuthorize("hasAuthority('role:assign')")
    public List<PermissionAssignmentResult> assignPermissions(@PathVariable("uuid") UUID uuid, @Valid @RequestBody Requests.AssignPermissions request) {
        return assignPermissionsToRoleUseCase.execute(new AssignPermissionsToRoleCommand(uuid, request.permissionUuids(), request.expiresAt(), request.reason()));
    }

    @DeleteMapping("/{uuid}/permissions/{permissionUuid}")
    @PreAuthorize("hasAuthority('role:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable("uuid") UUID uuid,
            @PathVariable("permissionUuid") UUID permissionUuid,
            @Valid @RequestBody Requests.Revoke request) {
        revokePermissionFromRoleUseCase.execute(uuid, permissionUuid, request.reason());
    }

}
