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
@RequestMapping("/api/permissions")
public class PermissionsController {
    private final CreatePermissionUseCase createPermissionUseCase;
    private final ListPermissionsUseCase listPermissionsUseCase;
    private final GetPermissionUseCase getPermissionUseCase;
    private final UpdatePermissionUseCase updatePermissionUseCase;
    private final DeletePermissionUseCase deletePermissionUseCase;
    private final ActivatePermissionUseCase activatePermissionUseCase;
    private final DeactivatePermissionUseCase deactivatePermissionUseCase;
    public PermissionsController(
            CreatePermissionUseCase createPermissionUseCase,
            ListPermissionsUseCase listPermissionsUseCase,
            GetPermissionUseCase getPermissionUseCase,
            UpdatePermissionUseCase updatePermissionUseCase,
            DeletePermissionUseCase deletePermissionUseCase,
            ActivatePermissionUseCase activatePermissionUseCase,
            DeactivatePermissionUseCase deactivatePermissionUseCase) {
        this.createPermissionUseCase = createPermissionUseCase;
        this.listPermissionsUseCase = listPermissionsUseCase;
        this.getPermissionUseCase = getPermissionUseCase;
        this.updatePermissionUseCase = updatePermissionUseCase;
        this.deletePermissionUseCase = deletePermissionUseCase;
        this.activatePermissionUseCase = activatePermissionUseCase;
        this.deactivatePermissionUseCase = deactivatePermissionUseCase;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResult create(@Valid @RequestBody Requests.CreatePermission request) {
        return createPermissionUseCase.execute(request.command());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public PageResult<PermissionResult> list(@RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="20") int size,
            @RequestParam(name="sort", defaultValue="createdAt,desc") String sort,
            @RequestParam(name="search", required=false) String search,
            @RequestParam(name="resource", required=false) String resource,
            @RequestParam(name="action", required=false) String action,
            @RequestParam(name="active", required=false) Boolean active) {
        return listPermissionsUseCase.execute(new PermissionFilterQuery(page, size, sort, search, resource, action, active));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('permission:read')")
    public PermissionResult get(@PathVariable("uuid") UUID uuid) {
        return getPermissionUseCase.execute(uuid);
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('permission:update')")
    public PermissionResult update(@PathVariable("uuid") UUID uuid, @Valid @RequestBody PermissionPatchRequest request) {
        return updatePermissionUseCase.execute(request.command(uuid));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority('permission:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("uuid") UUID uuid) {
        deletePermissionUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/activate")
    @PreAuthorize("hasAuthority('permission:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable("uuid") UUID uuid) {
        activatePermissionUseCase.execute(uuid);
    }

    @PostMapping("/{uuid}/deactivate")
    @PreAuthorize("hasAuthority('permission:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("uuid") UUID uuid) {
        deactivatePermissionUseCase.execute(uuid);
    }

}
