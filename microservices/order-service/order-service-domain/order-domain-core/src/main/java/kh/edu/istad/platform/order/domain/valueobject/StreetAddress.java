package kh.edu.istad.platform.order.domain.valueobject;

public record StreetAddress(String value) {
    public StreetAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }
        value = value.trim();
    }
}
