package istad.ha.iamservice.application.exception;

public final class ApplicationException extends RuntimeException {
    private final String code;
    private final java.util.Map<String, String> errors;
    public ApplicationException(String code, String message) { this(code, message, java.util.Map.of()); }
    public ApplicationException(String code, String message, java.util.Map<String, String> errors) {
        super(message); this.code = code; this.errors = java.util.Map.copyOf(errors);
    }
    public String code() { return code; }
    public java.util.Map<String, String> errors() { return errors; }
}
