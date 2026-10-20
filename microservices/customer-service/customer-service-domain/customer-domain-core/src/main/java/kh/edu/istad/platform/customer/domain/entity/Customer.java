package kh.edu.istad.platform.customer.domain.entity;

import kh.edu.istad.common.domain.entity.AggregateRoot;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.exception.CustomerDomainException;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;

import java.util.UUID;

/** Customer aggregate: validates contact details and controls identity, membership, and status changes. */
public class Customer extends AggregateRoot<CustomerId> {
    private String username;
    private String familyName;
    private String givenName;
    private Email email;
    private PhoneNumber phoneNumber;
    private UUID segmentId;
    private Long version;
    private CustomerStatus status;

    // Domain Critical Logic
    public void initiateCustomer() {
        validateCustomer();
        validateName(username, "username");
        validateName(familyName, "familyName");
        validateName(givenName, "givenName");
        if (email == null || phoneNumber == null) throw new IllegalArgumentException("Email and phone are required");
        username = username.trim().toLowerCase(java.util.Locale.ROOT);
        super.setId(new CustomerId(UUID.randomUUID()));
        status = CustomerStatus.ACTIVE;
    }

    private void validateCustomer() {
        if (super.getId() != null) {
            throw new CustomerDomainException("Customer id must be null");
        }
        if (status != null) {
            throw new CustomerDomainException("Customer status must be null");
        }
    }

    public void updateCustomer(String familyName, String givenName) {
        validateName(familyName, "familyName");
        validateName(givenName, "givenName");
        this.familyName = familyName;
        this.givenName = givenName;
    }

    public void deactivateCustomer() {
        if (status != CustomerStatus.ACTIVE) {
            throw new CustomerDomainException("Could not deactivate because customer status is not active");
        }
        status = CustomerStatus.INACTIVE;
    }


    // DELETED is a terminal state and can only be applied by the atomic soft-delete operation.
    public void changeStatus(CustomerStatus next) {
        if (status == CustomerStatus.DELETED || next == null || next == CustomerStatus.DELETED)
            throw new CustomerDomainException("Only ACTIVE and INACTIVE status transitions are allowed");
        status = next;
    }

    public void updateDetails(String username, String email, String phoneNumber) {
        if (username != null) {
            validateName(username, "username");
            this.username = username.trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (email != null) this.email = new Email(email);
        if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
    }

    private static void validateName(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 100)
            throw new IllegalArgumentException(field + " must contain 1 to 100 characters");
    }

    public Long getVersion() { return version; }

    public UUID getSegmentId() { return segmentId; }
    public void assignSegment(UUID segmentId) { this.segmentId = segmentId; }

    public String getUsername() {
        return username;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public Email getEmail() {
        return email;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    private Customer(Builder builder) {
        super.setId(builder.id);
        username = builder.username;
        familyName = builder.familyName;
        givenName = builder.givenName;
        email = builder.email;
        phoneNumber = builder.phoneNumber;
        status = builder.status;
        segmentId = builder.segmentId;
        version = builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private CustomerId id;
        private String username;
        private String familyName;
        private String givenName;
        private Email email;
        private PhoneNumber phoneNumber;
        private CustomerStatus status;
        private UUID segmentId;
        private Long version;

        private Builder() {
        }

        public Builder id(CustomerId val) {
            id = val;
            return this;
        }

        public Builder username(String val) {
            username = val;
            return this;
        }

        public Builder familyName(String val) {
            familyName = val;
            return this;
        }

        public Builder givenName(String val) {
            givenName = val;
            return this;
        }

        public Builder email(Email val) {
            email = val;
            return this;
        }

        public Builder phoneNumber(PhoneNumber val) {
            phoneNumber = val;
            return this;
        }

        public Builder status(CustomerStatus val) {
            status = val;
            return this;
        }

        public Builder version(Long value) { version = value; return this; }

        public Builder segmentId(UUID value) { segmentId = value; return this; }

        public Customer build() {
            return new Customer(this);
        }
    }
}
