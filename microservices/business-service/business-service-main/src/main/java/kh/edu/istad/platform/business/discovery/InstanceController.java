package kh.edu.istad.platform.business.discovery;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InstanceController {
    private final Environment environment;

    public InstanceController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/api/businesses/instance")
    public Map<String, String> instance() {
        return Map.of("service", environment.getRequiredProperty("spring.application.name"),
                "port", environment.getRequiredProperty("local.server.port"));
    }
}
