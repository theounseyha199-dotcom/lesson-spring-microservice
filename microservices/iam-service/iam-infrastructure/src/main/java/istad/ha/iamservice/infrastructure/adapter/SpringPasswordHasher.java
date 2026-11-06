package istad.ha.iamservice.infrastructure.adapter;

import istad.ha.iamservice.application.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class SpringPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;
    public SpringPasswordHasher(PasswordEncoder encoder) { this.encoder = encoder; }
    @Override public String hash(String password) { return encoder.encode(password); }
    @Override public boolean matches(String password, String hash) { return password != null && hash != null && encoder.matches(password, hash); }
}
