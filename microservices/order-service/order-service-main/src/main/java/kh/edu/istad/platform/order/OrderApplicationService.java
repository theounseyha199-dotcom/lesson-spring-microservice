package kh.edu.istad.platform.order;

import kh.edu.istad.swagger.OpenApiConfig;
import org.springframework.context.annotation.Import;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@Import(OpenApiConfig.class)
public class OrderApplicationService {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplicationService.class, args);

    }
}
