package kh.edu.istad.platform.customer.domain.dto;

import java.util.UUID;

public record initiateCustomerResult (
        UUID customerId,
        String userName,
        String familyName,
        String givenName,
        String email,
        String phoneNumber
){
}
