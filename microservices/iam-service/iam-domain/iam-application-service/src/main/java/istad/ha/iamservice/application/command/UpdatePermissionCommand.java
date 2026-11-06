package istad.ha.iamservice.application.command;

public record UpdatePermissionCommand(java.util.UUID permissionUuid, FieldUpdate<String> resource, FieldUpdate<String> action, FieldUpdate<String> description) {
    public UpdatePermissionCommand { resource = FieldUpdate.normalize(resource); action = FieldUpdate.normalize(action); description = FieldUpdate.normalize(description); }
}
