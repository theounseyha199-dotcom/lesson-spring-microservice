package istad.ha.iamservice.domain.rule;

import istad.ha.iamservice.domain.exception.DomainException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public final class Checks {
    private Checks() { }
    public static void require(boolean condition, String code, String message) {
        if (!condition) throw new DomainException(code, message);
    }
    public static String text(String value, String field, int max) {
        require(value != null && !value.isBlank(), "validation", field + " is required");
        value = value.strip();
        require(value.length() <= max, "validation", field + " exceeds " + max + " characters");
        return value;
    }
    public static String optional(String value, String field, int max) {
        return value == null ? null : text(value, field, max);
    }
    public static String email(String value) {
        value = text(value, "email", 255).toLowerCase(Locale.ROOT);
        require(value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "validation", "Invalid email");
        return value;
    }
    public static <T> T required(T value, String field) {
        require(value != null, "validation", field + " is required"); return value;
    }
    public static void id(Long value, String field) {
        require(value == null || value > 0, "validation", field + " must be positive");
    }
    public static long reference(Long value, String field) {
        required(value, field); id(value, field); return value;
    }
    public static String url(String value, String field) {
        if (value == null) return null;
        value = text(value, field, 500);
        try {
            URI uri = URI.create(value);
            require((Objects.equals(uri.getScheme(), "https") || Objects.equals(uri.getScheme(), "http"))
                    && uri.getHost() != null, "validation", field + " must be an absolute HTTP(S) URL");
        } catch (IllegalArgumentException ex) {
            throw new DomainException("validation", field + " is invalid");
        }
        return value;
    }
}
