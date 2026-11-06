package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.application.usecase.GetConsentClientUseCase;
import istad.ha.iamservice.web.view.BrowserUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Controller
public class ConsentPageController {
    private final GetConsentClientUseCase clients; private final BrowserUser current;
    public ConsentPageController(GetConsentClientUseCase clients, BrowserUser current) { this.clients = clients; this.current = current; }
    @GetMapping("/oauth2/consent") public String consent(@RequestParam("client_id") String clientId, @RequestParam("scope") String scope,
            @RequestParam("state") String state, Model model) {
        current.current();
        if (state.isBlank() || state.length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            var scopes = new TreeSet<>(Arrays.asList(scope.split(" +")));
            var client = clients.execute(clientId, scopes);
            model.addAttribute("client", client); model.addAttribute("clientId", clientId); model.addAttribute("state", state); return "consent";
        } catch (istad.ha.iamservice.application.exception.ApplicationException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    }
}
