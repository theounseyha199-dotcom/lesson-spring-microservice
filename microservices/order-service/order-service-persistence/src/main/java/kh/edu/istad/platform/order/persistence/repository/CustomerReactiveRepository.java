package kh.edu.istad.platform.order.persistence.repository;

import kh.edu.istad.platform.order.domain.entity.Customer;
import kh.edu.istad.platform.order.persistence.entity.CustomerEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface CustomerReactiveRepository extends R2dbcRepository<CustomerEntity, UUID> {
}
