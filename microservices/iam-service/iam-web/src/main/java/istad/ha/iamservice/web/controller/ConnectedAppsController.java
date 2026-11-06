package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.web.view.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class ConnectedAppsController {
    private final BrowserUser current; private final ListConnectedAppsUseCase list; private final RevokeConnectedAppUseCase revoke;
    public ConnectedAppsController(BrowserUser current, ListConnectedAppsUseCase list, RevokeConnectedAppUseCase revoke) { this.current = current; this.list = list; this.revoke = revoke; }
    @GetMapping("/profile/apps") public String apps(jakarta.servlet.http.HttpServletRequest request, Model model) { model.addAttribute("revoked", request.getParameterMap().containsKey("revoked")); model.addAttribute("apps", list.execute(current.current().username())); model.addAttribute("errorCode", ""); return "connected-apps"; }
    @GetMapping("/profile/apps/{id}/revoke") public String confirmation(@PathVariable("id") String id, Model model) {
        var app = list.execute(current.current().username()).stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        model.addAttribute("app", app); return "revoke-app";
    }
    @PostMapping("/profile/apps/{id}/revoke") public String revoke(@PathVariable("id") String id, Model model, HttpServletResponse response) {
        var user = current.current();
        try { revoke.execute(user.username(), id); return "redirect:/profile/apps?revoked"; }
        catch (RuntimeException ex) { response.setStatus(400); model.addAttribute("apps", list.execute(user.username())); model.addAttribute("errorCode", Forms.code(ex)); return "connected-apps"; }
    }
}
