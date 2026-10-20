package kh.edu.istad.platform.customer.persistence.mapper;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.persistence.document.CustomerDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
public interface CustomerPersistenceMapper {
    @Mapping(target = "segmentId", expression = "java(customer.getSegmentId() == null ? null : customer.getSegmentId().toString())")
    @Mapping(target = "customerId", source = "id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "newCustomer", constant = "true")
    CustomerDocument toDocument(Customer customer);

    @Mapping(target = "segmentId", expression = "java(document.segmentId() == null ? null : java.util.UUID.fromString(document.segmentId()))")
    @Mapping(target = "id", source = "customerId")
    Customer toDomain(CustomerDocument document);

    default String toString(CustomerId id) {
        return id == null ? null : id.value().toString();
    }

    default CustomerId toCustomerId(String id) {
        return id == null ? null : new CustomerId(UUID.fromString(id));
    }

    default String toString(Email email) {
        return email == null ? null : email.value();
    }

    default Email toEmail(String email) {
        return email == null ? null : new Email(email);
    }

    default String toString(PhoneNumber phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.value();
    }

    default PhoneNumber toPhoneNumber(String phoneNumber) {
        return phoneNumber == null ? null : new PhoneNumber(phoneNumber);
    }
}
