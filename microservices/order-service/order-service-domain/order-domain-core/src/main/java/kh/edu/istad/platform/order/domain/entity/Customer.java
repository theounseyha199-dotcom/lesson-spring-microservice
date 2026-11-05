package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.BaseEntity;
import kh.edu.istad.common.domain.valueobject.CustomerId;

import java.util.Objects;

public class Customer extends BaseEntity<CustomerId> {
    private final String userName;
    private final String familyName;
    private final String givenName;

    private Customer(Builder builder) {
        setId(Objects.requireNonNull(builder.id, "customerId is required"));
        userName = builder.userName;
        familyName = builder.familyName;
        givenName = builder.givenName;
    }

    public String getUserName() {
        return userName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }


    public static final class Builder {
        private CustomerId id;
        private String userName;
        private String familyName;
        private String givenName;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(CustomerId val) {
            id = val;
            return this;
        }

        public Builder userName(String val) {
            userName = val;
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

        public Customer build() {
            return new Customer(this);
        }
    }
}
