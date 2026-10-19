package kh.edu.istad.platform.order.persistence.repository;

import kh.edu.istad.platform.order.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {}
