package kh.edu.istad.platform.customer.persistence.document;

import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "customers")
public record CustomerDocument(
        @Id String customerId,
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber,
        CustomerStatus status,
        String segmentId,
        @org.springframework.data.annotation.Version Long version,
        @CreatedDate Instant createdAt,
        @LastModifiedDate Instant updatedAt,
        @CreatedBy String createdBy,
        @LastModifiedBy String updatedBy,
        String deletedBy,
        Instant deletedAt,
        // UUIDs are assigned before persistence; this flag identifies inserts and is never stored.
        @Transient Boolean newCustomer
) implements Persistable<String> {

    public CustomerDocument(String customerId, String username, String familyName,
                            String givenName, String email, String phoneNumber, CustomerStatus status) {
        this(customerId, username, familyName, givenName, email, phoneNumber, status, null, null,
                null, null, null, null, null, null, true);
    }

    public CustomerDocument(String customerId, String username, String familyName, String givenName,
                            String email, String phoneNumber, CustomerStatus status, Instant createdAt,
                            Instant updatedAt, String createdBy, String updatedBy, String deletedBy,
                            Instant deletedAt, Boolean newCustomer) {
        this(customerId, username, familyName, givenName, email, phoneNumber, status, null, null,
                createdAt, updatedAt, createdBy, updatedBy, deletedBy, deletedAt, newCustomer);
    }

    @Override
    public String getId() {
        return customerId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return Boolean.TRUE.equals(newCustomer);
    }

    public static CustomerDocument forUpdate(CustomerDocument customer, CustomerDocument existing) {
        return new CustomerDocument(customer.customerId(), customer.username(), customer.familyName(),
                customer.givenName(), customer.email(), customer.phoneNumber(), customer.status(), customer.segmentId(), customer.version(),
                existing.createdAt(), existing.updatedAt(), existing.createdBy(), existing.updatedBy(),
                existing.deletedBy(), existing.deletedAt(), false);
    }
}
