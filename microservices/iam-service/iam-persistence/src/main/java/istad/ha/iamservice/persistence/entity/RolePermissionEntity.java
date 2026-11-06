package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "role_permissions") @Getter @Setter
public class RolePermissionEntity extends AssignmentEntity {
    @Column(name = "role_id", nullable = false, updatable = false) private long roleId;
    @Column(name = "permission_id", nullable = false, updatable = false) private long permissionId;
}
