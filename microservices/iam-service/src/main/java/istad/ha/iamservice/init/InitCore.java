package istad.ha.iamservice.init;

import istad.ha.iamservice.domain.Permission;
import istad.ha.iamservice.domain.Role;
import istad.ha.iamservice.domain.User;
import istad.ha.iamservice.feature.permission.PermissionRepository;
import istad.ha.iamservice.feature.role.RoleJpaRepository;
import istad.ha.iamservice.feature.user.UserJpaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InitCore {

    private final PermissionRepository permissionRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // 1. Initialize permissions
        Permission readPermission = permissionRepository.findByPermission("user:read")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permission("user:read")
                        .description("Read user profiles and data")
                        .build()));

        Permission writePermission = permissionRepository.findByPermission("user:write")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permission("user:write")
                        .description("Create and update user data")
                        .build()));

        Permission deletePermission = permissionRepository.findByPermission("user:delete")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permission("user:delete")
                        .description("Delete user data")
                        .build()));

        // 2. Initialize roles
        Role adminRole = roleJpaRepository.findByRole("ADMIN")
                .orElseGet(() -> roleJpaRepository.save(Role.builder()
                        .role("ADMIN")
                        .description("Administrator with full permissions")
                        .permissions(Set.of(readPermission, writePermission, deletePermission))
                        .build()));

        Role userRole = roleJpaRepository.findByRole("USER")
                .orElseGet(() -> roleJpaRepository.save(Role.builder()
                        .role("USER")
                        .description("Regular user with read permission")
                        .permissions(Set.of(readPermission))
                        .build()));

        // 3. Initialize admin user
        if (!userJpaRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@istad.co")
                    .emailVerified(true)
                    .familyName("System")
                    .givenName("Admin")
                    .gender("Other")
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(true)
                    .roles(Set.of(adminRole))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userJpaRepository.save(admin);
        }

        // 4. Initialize regular user
        if (!userJpaRepository.existsByUsername("user")) {
            User user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .username("user")
                    .password(passwordEncoder.encode("password"))
                    .email("user@istad.co")
                    .emailVerified(true)
                    .familyName("Standard")
                    .givenName("User")
                    .gender("Male")
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(true)
                    .roles(Set.of(userRole))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userJpaRepository.save(user);
        }

        // 5. Initialize theounseyha user
        if (!userJpaRepository.existsByUsername("theounseyha")) {
            User theounseyha = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .username("theounseyha")
                    .password(passwordEncoder.encode("seyha12345"))
                    .email("theounseyha@istad.co")
                    .emailVerified(true)
                    .familyName("Theoun")
                    .givenName("Seyha")
                    .gender("Male")
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(true)
                    .roles(Set.of(adminRole, userRole))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userJpaRepository.save(theounseyha);
        }
    }
}
