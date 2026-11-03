package istad.ha.gatewayserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.cloud.consul.enabled=false")
class GatewayServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
