package kh.edu.istad.platform.order.persistence;

import kh.edu.istad.platform.order.application.OrderRepository;
import kh.edu.istad.platform.order.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
public class OrderJpaRepositoryAdapter implements OrderRepository {
    private final SpringDataOrderJpaRepository jpa;

    public OrderJpaRepositoryAdapter(SpringDataOrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        jpa.save(OrderJpaEntity.from(order));
        return order;
    }
}
