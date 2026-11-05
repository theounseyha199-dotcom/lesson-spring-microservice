package kh.edu.istad.platform.business;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/businesses/demo")
public class DemoController {

    @GetMapping
    public Flux<String> demo() {
        return Flux.just("Hello", "World", "From", "Business", "Service");
    }
}
