package istad.ha.iamservice.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("csrfParameterName", csrf.getParameterName());
        model.addAttribute("csrfToken", csrf.getToken());
        model.addAttribute("loginError", request.getParameterMap().containsKey("error"));
        model.addAttribute("loggedOut", request.getParameterMap().containsKey("logout"));
        model.addAttribute("oauth2Error", request.getParameterMap().containsKey("oauth2error"));
        model.addAttribute("registered", request.getParameterMap().containsKey("registered"));
        return "login";
    }
}
