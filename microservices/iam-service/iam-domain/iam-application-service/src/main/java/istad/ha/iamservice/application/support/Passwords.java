package istad.ha.iamservice.application.support;

public final class Passwords {
    private Passwords() { }
    public static void confirm(String password, String confirmation) {
        if (password == null || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new istad.ha.iamservice.application.exception.ApplicationException("validation", "Invalid new password",
                    java.util.Map.of("password", "Use at least 12 characters and at most 72 UTF-8 bytes"));
        }
        if (!password.equals(confirmation)) throw new istad.ha.iamservice.application.exception.ApplicationException(
                "validation", "Passwords do not match", java.util.Map.of("confirmPassword", "Passwords do not match"));
    }
}
