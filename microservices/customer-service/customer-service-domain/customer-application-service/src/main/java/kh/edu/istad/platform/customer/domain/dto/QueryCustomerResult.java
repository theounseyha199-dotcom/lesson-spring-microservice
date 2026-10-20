package kh.edu.istad.platform.customer.domain.dto;

import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;

import java.util.UUID;

public record QueryCustomerResult(
        UUID customerId,
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber,
        CustomerStatus status,
        java.util.UUID segmentId
) {
}
