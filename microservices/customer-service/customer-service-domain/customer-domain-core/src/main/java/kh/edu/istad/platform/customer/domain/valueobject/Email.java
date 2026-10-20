package kh.edu.istad.platform.customer.domain.valueobject;
import java.util.Locale;
public record Email(String value) {
    public Email {
        if (value == null || value.length() > 254 || !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("A valid email is required");
        value = value.toLowerCase(Locale.ROOT);
    }
}
