package kh.edu.istad.platform.customer.persistence.config;

import kh.edu.istad.platform.customer.persistence.convertor.CustomerIdReadConverter;
import kh.edu.istad.platform.customer.persistence.convertor.CustomerIdWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@Configuration
@EnableReactiveMongoAuditing(auditorAwareRef = "auditorAwareConfig")
public class MongoConfig {
    @Bean
    public MongoCustomConversions bean() {
        return MongoCustomConversions.create(
                adapter->{
                    adapter.registerConverter(new CustomerIdReadConverter());
                    adapter.registerConverter(new CustomerIdWriteConverter());
                }
        );
    }
}
