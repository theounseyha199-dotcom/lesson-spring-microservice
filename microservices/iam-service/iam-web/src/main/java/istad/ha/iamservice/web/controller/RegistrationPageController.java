package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.application.command.RegisterUserCommand;
import istad.ha.iamservice.application.usecase.RegisterUserUseCase;
import istad.ha.iamservice.web.view.Forms;
import istad.ha.iamservice.domain.rule.Checks;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
@Controller
public class RegistrationPageController {
    private final RegisterUserUseCase register;
    public RegistrationPageController(RegisterUserUseCase register) { this.register = register; }
    @GetMapping("/register") public String form(Model model) { model.addAttribute("values", Map.of()); model.addAttribute("errors", Map.of()); model.addAttribute("errorCode", ""); return "register"; }
    @PostMapping("/register") public String register(@RequestParam Map<String,String> values, Model model, HttpServletResponse response) {
        var errors = new LinkedHashMap<String,String>();
        for (String key : List.of("username", "email", "givenName", "familyName", "password", "confirmPassword"))
            if (values.getOrDefault(key, "").isBlank()) errors.put(key, "validation.required");
        if (!values.getOrDefault("username", "").matches("[A-Za-z0-9._-]{3,50}")) errors.put("username", "validation.username");
        try { Checks.email(values.get("email")); } catch (RuntimeException ex) { errors.put("email", "validation.email"); }
        for (String key : List.of("givenName", "familyName")) if (values.getOrDefault(key, "").length() > 100) errors.put(key, "validation.name");
        try { istad.ha.iamservice.application.support.Passwords.confirm(values.get("password"), values.get("confirmPassword")); }
        catch (istad.ha.iamservice.application.exception.ApplicationException ex) { errors.put(ex.errors().containsKey("confirmPassword") ? "confirmPassword" : "password", ex.errors().containsKey("confirmPassword") ? "validation.confirm" : "validation.password"); }
        String error = "validation";
        if (errors.isEmpty()) try {
            register.execute(new RegisterUserCommand(values.get("username"), values.get("email"), values.get("password"), values.get("confirmPassword"),
                    values.get("givenName"), values.get("familyName"), values.getOrDefault("locale", "en")));
            return "redirect:/login?registered";
        } catch (RuntimeException ex) { error = Forms.code(ex); }
        response.setStatus(400); model.addAttribute("values", Forms.values(values)); model.addAttribute("errors", errors); model.addAttribute("errorCode", error); return "register";
    }
}
