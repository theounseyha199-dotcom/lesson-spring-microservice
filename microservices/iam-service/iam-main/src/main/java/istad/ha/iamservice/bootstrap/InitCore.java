package istad.ha.iamservice.bootstrap;

import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.domain.model.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Component
public class InitCore implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final AssignmentRepository assignments;
    private final PasswordEncoder encoder;
    private final String adminPassword, userPassword, ownerPassword;
    public InitCore(UserRepository users, RoleRepository roles, PermissionRepository permissions, AssignmentRepository assignments, PasswordEncoder encoder,
            @org.springframework.beans.factory.annotation.Value("${app.iam.bootstrap-admin-password}") String adminPassword,
            @org.springframework.beans.factory.annotation.Value("${app.iam.password}") String userPassword,
            @org.springframework.beans.factory.annotation.Value("${app.iam.bootstrap-owner-password}") String ownerPassword) {
        this.users = users; this.roles = roles; this.permissions = permissions; this.assignments = assignments; this.encoder = encoder;
        org.springframework.util.Assert.hasText(adminPassword,"IAM_BOOTSTRAP_ADMIN_PASSWORD is required");
        org.springframework.util.Assert.hasText(userPassword,"IAM_PASSWORD is required");
        org.springframework.util.Assert.hasText(ownerPassword,"IAM_BOOTSTRAP_OWNER_PASSWORD is required");
        this.adminPassword=adminPassword; this.userPassword=userPassword; this.ownerPassword=ownerPassword;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        for (var definition : SystemDefaults.permissions("system", now)) {
            if (permissions.findByName(definition.name()).isEmpty()) permissions.save(definition);
        }
        for (var definition : SystemDefaults.roles("system", now)) {
            var role = roles.findByName(definition.name()).orElseGet(() -> roles.save(definition));
            if (!role.assignable()) continue;
            var existing = assignments.findRolePermissions(Set.of(role.id()));
            for (var name : SystemDefaults.permissionNamesFor(role.name())) {
                var permission = permissions.findByName(name).orElseThrow();
                if (permission.assignable() && existing.stream().noneMatch(a -> a.permissionId() == permission.id())) {
                    assignments.save(RolePermission.assign(role, permission, "system", now, null, "System default"));
                }
            }
        }
        seedUser("admin", "admin@istad.co", adminPassword, "Admin", "System", List.of("ADMIN"), now);
        seedUser("user", "user@istad.co", userPassword, "User", "Standard", List.of("USER"), now);
        seedUser("theounseyha", "theounseyha@istad.co", ownerPassword, "Seyha", "Theoun", List.of("ADMIN", "USER"), now);
    }
    private void seedUser(String username, String email, String password, String givenName, String familyName, List<String> roleNames, Instant now) {
        // Existing users are deliberately not modified or re-granted roles on restart.
        if (users.usernameExists(username)) return;
        var user = users.save(User.create(username, email, encoder.encode(password), UserProfile.basic(givenName, familyName, "en"), "system", now));
        for (var name : roleNames) {
            var role = roles.findByName(name).orElseThrow();
            assignments.save(UserRole.assign(user, role, "system", now, null, "Initial user role"));
        }
    }
}
