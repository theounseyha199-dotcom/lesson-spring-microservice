package kh.edu.istad.platform.customer.domain.dto;
import java.util.UUID;
public record InitiateCustomerCommand(String username, String familyName, String givenName,
                                     String email, String phoneNumber, UUID segmentId) {
    public InitiateCustomerCommand(String username, String familyName, String givenName, String email, String phoneNumber) {
        this(username, familyName, givenName, email, phoneNumber, null);
    }
}
