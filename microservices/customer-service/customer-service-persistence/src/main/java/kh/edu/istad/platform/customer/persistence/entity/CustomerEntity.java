package kh.edu.istad.platform.customer.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "username", nullable = false)
    private String username;
    @Column(name = "family_name", nullable = false)
    private String familyName;
    @Column(name = "given_name", nullable = false)
    private String givenName;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
}
