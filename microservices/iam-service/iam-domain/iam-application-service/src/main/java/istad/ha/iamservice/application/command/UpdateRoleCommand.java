package istad.ha.iamservice.application.command;

public record UpdateRoleCommand(java.util.UUID roleUuid, FieldUpdate<String> name, FieldUpdate<String> description) {
    public UpdateRoleCommand { name = FieldUpdate.normalize(name); description = FieldUpdate.normalize(description); }
}
