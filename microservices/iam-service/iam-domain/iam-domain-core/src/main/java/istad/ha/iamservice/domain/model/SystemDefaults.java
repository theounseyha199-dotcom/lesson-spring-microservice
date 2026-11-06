package istad.ha.iamservice.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Definitions only. A later persistence seed resolves IDs and creates audited assignments. */
public final class SystemDefaults {
    private SystemDefaults() { }
    public static List<Role> roles(String actor, Instant now) {
        return List.of(Role.create("ADMIN", "Administrator", true, actor, now),
                Role.create("USER", "Registered user", true, actor, now));
    }
    public static List<Permission> permissions(String actor, Instant now) {
        var result = new ArrayList<Permission>();
        for (String resource : List.of("user", "role", "permission")) {
            var actions = resource.equals("permission") ? List.of("create", "read", "update", "delete")
                    : List.of("create", "read", "update", "delete", "assign");
            for (String action : actions) result.add(Permission.create(resource, action,
                    action + " " + resource, true, actor, now));
        }
        return List.copyOf(result);
    }
    public static List<String> permissionNamesFor(String roleName) {
        if ("USER".equals(roleName)) return List.of("user:read", "role:read", "permission:read");
        istad.ha.iamservice.domain.rule.Checks.require("ADMIN".equals(roleName), "validation", "Unknown system role");
        return permissions("system", Instant.EPOCH).stream().map(Permission::name).toList();
    }
}
