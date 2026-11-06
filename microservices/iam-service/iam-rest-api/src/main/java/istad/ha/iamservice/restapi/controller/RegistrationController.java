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
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegisterUserUseCase registerUserUseCase;
    public RegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.User register(@Valid @RequestBody Requests.Register request) {
        return Responses.user(registerUserUseCase.execute(request.command()));
    }

}
