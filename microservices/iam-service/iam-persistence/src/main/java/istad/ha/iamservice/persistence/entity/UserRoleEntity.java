package istad.ha.iamservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "user_roles") @Getter @Setter
public class UserRoleEntity extends AssignmentEntity {
    @Column(name = "user_id", nullable = false, updatable = false) private long userId;
    @Column(name = "role_id", nullable = false, updatable = false) private long roleId;
}
