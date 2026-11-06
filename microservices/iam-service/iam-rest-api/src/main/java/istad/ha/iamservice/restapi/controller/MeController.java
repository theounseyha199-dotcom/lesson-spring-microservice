package istad.ha.iamservice.restapi.controller;

import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.query.*;
import istad.ha.iamservice.application.result.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.restapi.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ListUserIdentitiesUseCase listUserIdentitiesUseCase;
    private final UnlinkIdentityUseCase unlinkIdentityUseCase;
    public MeController(
            GetUserUseCase getUserUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            ListUserIdentitiesUseCase listUserIdentitiesUseCase,
            UnlinkIdentityUseCase unlinkIdentityUseCase) {
        this.getUserUseCase = getUserUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.listUserIdentitiesUseCase = listUserIdentitiesUseCase;
        this.unlinkIdentityUseCase = unlinkIdentityUseCase;
    }
    private UUID subject(org.springframework.security.oauth2.jwt.Jwt jwt) {
        try {
            var uuid = UUID.fromString(jwt.getSubject());
            if (!uuid.toString().equalsIgnoreCase(jwt.getSubject())) throw new IllegalArgumentException();
            return uuid;
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new org.springframework.security.access.AccessDeniedException("An internal user subject is required");
        }
    }
    @GetMapping
    public Responses.User get(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return Responses.user(getUserUseCase.execute(subject(jwt)));
    }

    @PatchMapping
    public Responses.User update(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt,
            @Valid @RequestBody ProfileRequest request) {
        return Responses.user(updateUserProfileUseCase.execute(request.command(subject(jwt))));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt,
            @Valid @RequestBody Requests.ChangePassword request) {
        changePasswordUseCase.execute(request.command(subject(jwt)));
    }

    @GetMapping("/identities")
    public List<Responses.Identity> identities(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return listUserIdentitiesUseCase.execute(subject(jwt)).stream().map(Responses::identity).toList();
    }

    @DeleteMapping("/identities/{identityUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt,
            @PathVariable("identityUuid") UUID identityUuid) {
        unlinkIdentityUseCase.execute(subject(jwt), identityUuid);
    }

}
