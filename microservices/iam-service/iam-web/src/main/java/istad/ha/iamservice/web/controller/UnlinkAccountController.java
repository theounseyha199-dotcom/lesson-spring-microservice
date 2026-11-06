package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.web.view.*;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class UnlinkAccountController {
    private final BrowserUser current; private final ListUserIdentitiesUseCase list; private final UnlinkIdentityUseCase unlink;
    public UnlinkAccountController(BrowserUser current,ListUserIdentitiesUseCase list,UnlinkIdentityUseCase unlink) { this.current=current; this.list=list; this.unlink=unlink; }
    @GetMapping("/profile/accounts/{id}/unlink") public String confirm(@PathVariable("id") UUID id, Model model) {
        var identity=list.execute(current.current().uuid()).stream().filter(i -> i.uuid().equals(id)).findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        model.addAttribute("identity",identity); return "unlink-account";
    }
    @PostMapping("/profile/accounts/{id}/unlink") public String unlink(@PathVariable("id") UUID id,RedirectAttributes redirect) {
        var user=current.current();
        try { unlink.execute(user.uuid(),id); return "redirect:/profile/accounts?unlinked"; }
        catch(RuntimeException ex) { redirect.addFlashAttribute("errorCode",Forms.code(ex)); return "redirect:/profile/accounts?error"; }
    }
}
