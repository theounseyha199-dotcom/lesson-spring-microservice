package kh.edu.istad.platform.customer.domain.dto;
import java.util.UUID;
public record UpdateCustomerCommand(UUID customerId, String familyName, String givenName,
                                    String username, String email, String phoneNumber) {
    public UpdateCustomerCommand(UUID customerId, String familyName, String givenName) {
        this(customerId, familyName, givenName, null, null, null);
    }
}
