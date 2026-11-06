package istad.ha.iamservice.infrastructure.config;

import istad.ha.iamservice.application.port.*;
import istad.ha.iamservice.application.support.UseCaseContext;
import istad.ha.iamservice.application.usecase.*;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class ApplicationUseCaseConfig {
    @Bean public Clock iamClock() { return Clock.systemUTC(); }
    @Bean public UseCaseContext useCaseContext(UserRepository users, RoleRepository roles, PermissionRepository permissions,
            IdentityRepository identities, AssignmentRepository assignments, PasswordHasher passwords, CurrentActorProvider actors,
            TransactionRunner transactions, Clock clock) {
        return new UseCaseContext(users, roles, permissions, identities, assignments, passwords, actors, transactions, clock);
    }
    @Bean public RegisterUserUseCase registerUserUseCase(UseCaseContext context) { return new RegisterUserUseCase(context); }
    @Bean public CreateUserUseCase createUserUseCase(UseCaseContext context) { return new CreateUserUseCase(context); }
    @Bean public GetUserUseCase getUserUseCase(UseCaseContext context) { return new GetUserUseCase(context); }
    @Bean public ListUsersUseCase listUsersUseCase(UseCaseContext context) { return new ListUsersUseCase(context); }
    @Bean public UpdateUserProfileUseCase updateUserProfileUseCase(UseCaseContext context) { return new UpdateUserProfileUseCase(context); }
    @Bean public ChangePasswordUseCase changePasswordUseCase(UseCaseContext context) { return new ChangePasswordUseCase(context); }
    @Bean public ResetUserPasswordUseCase resetUserPasswordUseCase(UseCaseContext context) { return new ResetUserPasswordUseCase(context); }
    @Bean public EnableUserUseCase enableUserUseCase(UseCaseContext context) { return new EnableUserUseCase(context); }
    @Bean public DisableUserUseCase disableUserUseCase(UseCaseContext context) { return new DisableUserUseCase(context); }
    @Bean public LockUserUseCase lockUserUseCase(UseCaseContext context) { return new LockUserUseCase(context); }
    @Bean public UnlockUserUseCase unlockUserUseCase(UseCaseContext context) { return new UnlockUserUseCase(context); }
    @Bean public DeleteUserUseCase deleteUserUseCase(UseCaseContext context) { return new DeleteUserUseCase(context); }
    @Bean public AssignRolesToUserUseCase assignRolesToUserUseCase(UseCaseContext context) { return new AssignRolesToUserUseCase(context); }
    @Bean public RevokeRoleFromUserUseCase revokeRoleFromUserUseCase(UseCaseContext context) { return new RevokeRoleFromUserUseCase(context); }
    @Bean public GrantPermissionsToUserUseCase grantPermissionsToUserUseCase(UseCaseContext context) { return new GrantPermissionsToUserUseCase(context); }
    @Bean public RevokePermissionFromUserUseCase revokePermissionFromUserUseCase(UseCaseContext context) { return new RevokePermissionFromUserUseCase(context); }
    @Bean public GetUserAuthoritiesUseCase getUserAuthoritiesUseCase(UseCaseContext context) { return new GetUserAuthoritiesUseCase(context); }
    @Bean public LoadUserForAuthenticationUseCase loadUserForAuthenticationUseCase(UseCaseContext context) { return new LoadUserForAuthenticationUseCase(context); }
    @Bean public RecordLoginAttemptUseCase recordLoginAttemptUseCase(UseCaseContext context) { return new RecordLoginAttemptUseCase(context); }
    @Bean public CreateRoleUseCase createRoleUseCase(UseCaseContext context) { return new CreateRoleUseCase(context); }
    @Bean public GetRoleUseCase getRoleUseCase(UseCaseContext context) { return new GetRoleUseCase(context); }
    @Bean public ListRolesUseCase listRolesUseCase(UseCaseContext context) { return new ListRolesUseCase(context); }
    @Bean public UpdateRoleUseCase updateRoleUseCase(UseCaseContext context) { return new UpdateRoleUseCase(context); }
    @Bean public ActivateRoleUseCase activateRoleUseCase(UseCaseContext context) { return new ActivateRoleUseCase(context); }
    @Bean public DeactivateRoleUseCase deactivateRoleUseCase(UseCaseContext context) { return new DeactivateRoleUseCase(context); }
    @Bean public DeleteRoleUseCase deleteRoleUseCase(UseCaseContext context) { return new DeleteRoleUseCase(context); }
    @Bean public AssignPermissionsToRoleUseCase assignPermissionsToRoleUseCase(UseCaseContext context) { return new AssignPermissionsToRoleUseCase(context); }
    @Bean public RevokePermissionFromRoleUseCase revokePermissionFromRoleUseCase(UseCaseContext context) { return new RevokePermissionFromRoleUseCase(context); }
    @Bean public CreatePermissionUseCase createPermissionUseCase(UseCaseContext context) { return new CreatePermissionUseCase(context); }
    @Bean public GetPermissionUseCase getPermissionUseCase(UseCaseContext context) { return new GetPermissionUseCase(context); }
    @Bean public ListPermissionsUseCase listPermissionsUseCase(UseCaseContext context) { return new ListPermissionsUseCase(context); }
    @Bean public UpdatePermissionUseCase updatePermissionUseCase(UseCaseContext context) { return new UpdatePermissionUseCase(context); }
    @Bean public ActivatePermissionUseCase activatePermissionUseCase(UseCaseContext context) { return new ActivatePermissionUseCase(context); }
    @Bean public DeactivatePermissionUseCase deactivatePermissionUseCase(UseCaseContext context) { return new DeactivatePermissionUseCase(context); }
    @Bean public DeletePermissionUseCase deletePermissionUseCase(UseCaseContext context) { return new DeletePermissionUseCase(context); }
    @Bean public LinkIdentityUseCase linkIdentityUseCase(UseCaseContext context) { return new LinkIdentityUseCase(context); }
    @Bean public UnlinkIdentityUseCase unlinkIdentityUseCase(UseCaseContext context) { return new UnlinkIdentityUseCase(context); }
    @Bean public ListUserIdentitiesUseCase listUserIdentitiesUseCase(UseCaseContext context) { return new ListUserIdentitiesUseCase(context); }
    @Bean public FindOrProvisionFederatedUserUseCase findOrProvisionFederatedUserUseCase(UseCaseContext context) { return new FindOrProvisionFederatedUserUseCase(context); }
}
