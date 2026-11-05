package istad.ha.iamservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Map.of("status", "IAM Server is running");
        }
        return Map.of(
                "status", "success",
                "message", "Login successful!",
                "username", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities()
        );
    }
}
