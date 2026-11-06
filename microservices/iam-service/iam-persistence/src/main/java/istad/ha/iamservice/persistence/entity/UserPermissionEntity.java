package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "user_permissions") @Getter @Setter
public class UserPermissionEntity extends AssignmentEntity {
    @Column(name = "user_id", nullable = false, updatable = false) private long userId;
    @Column(name = "permission_id", nullable = false, updatable = false) private long permissionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 5) private istad.ha.iamservice.domain.model.PermissionEffect effect;
}
