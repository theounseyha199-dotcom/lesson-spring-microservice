package kh.edu.istad.platform.order.persistence;

import java.time.Duration;
import kh.edu.istad.platform.order.OrderApplicationService;
import kh.edu.istad.platform.order.persistence.repository.BusinessReactiveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrderApplicationService.class, properties = {
        "spring.cloud.consul.enabled=false",
        "spring.cloud.consul.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "service.message=Repository integration test",
        "service.env=test",
        "service.version=test",
        "global.version=test"
})
class BusinessRepositoryTest {

    @Autowired
    private BusinessReactiveRepository businessReactiveRepository;

    @Test
    void findAllBusinesses() {
        var businesses = businessReactiveRepository.findAll()
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(businesses).isNotNull();
    }
}
