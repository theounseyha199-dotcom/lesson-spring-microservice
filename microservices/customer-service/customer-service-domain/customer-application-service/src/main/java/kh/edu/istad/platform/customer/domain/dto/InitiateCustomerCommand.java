package kh.edu.istad.platform.customer.domain.dto;

public record InitiateCustomerCommand(
   String userName,
   String familyName,
   String givenName,
   String email,
   String phoneNumber
){
}
