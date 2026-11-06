package istad.ha.iamservice.application.command;

import java.util.*;
import java.time.Instant;
import istad.ha.iamservice.domain.model.*;

public record RegisterUserCommand(String username, String email, String password, String confirmPassword, String givenName, String familyName, String locale) {
    @Override public String toString() { return "RegisterUserCommand[redacted]"; }
}
