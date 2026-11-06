package istad.ha.iamservice.restapi.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import istad.ha.iamservice.application.command.*;

public final class PermissionPatchRequest implements Requests.Strict {
    @Size(max=50) private String resource;
    private boolean resourceSupplied;
    @JsonSetter("resource") public void setResource(String value) { resource = value; resourceSupplied = true; }
    @Size(max=50) private String action;
    private boolean actionSupplied;
    @JsonSetter("action") public void setAction(String value) { action = value; actionSupplied = true; }
    @Size(max=255) private String description;
    private boolean descriptionSupplied;
    @JsonSetter("description") public void setDescription(String value) { description = value; descriptionSupplied = true; }
    public UpdatePermissionCommand command(UUID uuid) {
        return new UpdatePermissionCommand(uuid, resourceSupplied ? FieldUpdate.set(resource) : FieldUpdate.unchanged(), actionSupplied ? FieldUpdate.set(action) : FieldUpdate.unchanged(), descriptionSupplied ? FieldUpdate.set(description) : FieldUpdate.unchanged());
    }
}
