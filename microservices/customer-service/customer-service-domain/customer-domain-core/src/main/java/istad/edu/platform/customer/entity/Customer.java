package istad.edu.platform.customer.entity;

import istad.edu.platform.customer.valueobject.CustomerStatus;
import istad.edu.platform.customer.valueobject.Email;
import istad.edu.platform.customer.valueobject.PhoneNumber;
import kh.edu.istad.common.domain.Exception.CustomerDomainException;
import kh.edu.istad.common.domain.entity.AggregateRoot;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class Customer extends AggregateRoot<CustomerId> {

    private final String username;
    private String familyName;
    private  String givenName;
    private final Email email;
    private final PhoneNumber phoneNumber;
    private  CustomerStatus customerStatus;


    //Domain Critical Logic

    public void initiateCustomer() {


        validateCustomer();
        super.setId(new CustomerId(java.util.UUID.randomUUID()));
        customerStatus = CustomerStatus.ACTIVE;
    }
    public void validateCustomer(){
        if (super.getId() == null) {
            throw new CustomerDomainException("Customer ID cannot be null");
        }
        if (customerStatus != null){
            throw new CustomerDomainException("Customer status cannot be null");
        }

    }

    // update customer information

    public void updateCustomer (String familyName , String givenName){
        if(familyName == null || givenName == null){
            throw new CustomerDomainException("Family name cannot be null or empty");
        }
        this.familyName = familyName;
        this.givenName = givenName;
    }

    // deactivate customer

    public void deactivateCustomer() {

       if ((customerStatus!= CustomerStatus.ACTIVE)) {
            throw new CustomerDomainException("Could not deactivate customer because the customer is not active.");
        }
       customerStatus = CustomerStatus.INACTIVE;

    }

    private Customer(Builder builder) {
        username = builder.username;
        familyName = builder.familyName;
        givenName = builder.givenName;
        email = builder.email;
        phoneNumber = builder.phoneNumber;
        customerStatus = builder.customerStatus;
    }



    public static final class Builder {
        private CustomerId id;
        private String username;
        private String familyName;
        private String givenName;
        private Email email;
        private PhoneNumber phoneNumber;
        private CustomerStatus customerStatus;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
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

        public Builder customerStatus(CustomerStatus val) {
            customerStatus = val;
            return this;
        }

        public Customer build() {
            return new Customer(this);
        }
    }

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

    public CustomerStatus getCustomerStatus() {
        return customerStatus;
    }


}