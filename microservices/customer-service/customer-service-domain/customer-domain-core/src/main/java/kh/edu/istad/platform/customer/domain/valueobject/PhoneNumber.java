package kh.edu.istad.platform.customer.domain.valueobject;
public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value == null || !value.matches("\\+?[0-9]{8,15}"))
            throw new IllegalArgumentException("Phone number must contain 8 to 15 digits with an optional + prefix");
    }
}
