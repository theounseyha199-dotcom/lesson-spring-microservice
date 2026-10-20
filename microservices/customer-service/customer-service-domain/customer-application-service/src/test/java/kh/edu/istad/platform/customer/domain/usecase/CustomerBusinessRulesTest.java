package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.valueobject.*;
import kh.edu.istad.platform.customer.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerBusinessRulesTest {
    private Customer customer() {
        var c = Customer.builder().username("SOKHA").familyName("Chan").givenName("Sokha")
                .email(new Email("SOKHA@example.com")).phoneNumber(new PhoneNumber("+85512345678")).build();
        c.initiateCustomer();
        return c;
    }
    @Test void normalizesIdentityAndStartsActive() {
        var c = customer();
        assertEquals("sokha", c.getUsername());
        assertEquals("sokha@example.com", c.getEmail().value());
        assertEquals(CustomerStatus.ACTIVE, c.getStatus());
        assertNotNull(c.getId());
    }
    @Test void allowsReactivationButCannotSetDeletedStatus() {
        var c = customer();
        c.deactivateCustomer();
        assertThrows(CustomerDomainException.class, c::deactivateCustomer);
        c.changeStatus(CustomerStatus.ACTIVE);
        assertEquals(CustomerStatus.ACTIVE, c.getStatus());
        assertThrows(CustomerDomainException.class, () -> c.changeStatus(CustomerStatus.DELETED));
    }
    @Test void validatesContactDetailsAndNames() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("abc"));
        assertThrows(IllegalArgumentException.class, () -> customer().updateCustomer(" ", "Sokha"));
        assertThrows(IllegalArgumentException.class, () -> customer().updateDetails(" ", null, null));
    }
}
