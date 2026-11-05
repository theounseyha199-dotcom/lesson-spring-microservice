package kh.edu.istad.platform.order.client.dto;

public record PostCustomer (
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber,
        String customerStatus,
        String customerSegmentType
){
}
