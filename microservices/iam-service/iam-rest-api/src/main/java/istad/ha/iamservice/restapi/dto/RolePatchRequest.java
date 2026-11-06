package istad.ha.iamservice.restapi.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import istad.ha.iamservice.application.command.*;

public final class RolePatchRequest implements Requests.Strict {
    @Size(max=50) private String name;
    private boolean nameSupplied;
    @JsonSetter("name") public void setName(String value) { name = value; nameSupplied = true; }
    @Size(max=255) private String description;
    private boolean descriptionSupplied;
    @JsonSetter("description") public void setDescription(String value) { description = value; descriptionSupplied = true; }
    public UpdateRoleCommand command(UUID uuid) {
        return new UpdateRoleCommand(uuid, nameSupplied ? FieldUpdate.set(name) : FieldUpdate.unchanged(), descriptionSupplied ? FieldUpdate.set(description) : FieldUpdate.unchanged());
    }
}
