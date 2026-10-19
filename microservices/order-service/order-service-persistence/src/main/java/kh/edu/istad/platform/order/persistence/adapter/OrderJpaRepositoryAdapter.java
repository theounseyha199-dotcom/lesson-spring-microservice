package kh.edu.istad.platform.order.persistence.adapter;

import kh.edu.istad.platform.order.domain.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import org.springframework.stereotype.Repository;
import kh.edu.istad.platform.order.persistence.repository.SpringDataOrderJpaRepository;
import kh.edu.istad.platform.order.persistence.mapper.OrderPersistenceMapper;

@Repository
public class OrderJpaRepositoryAdapter implements OrderRepository {
    private final SpringDataOrderJpaRepository jpa;
    private final OrderPersistenceMapper mapper;

    public OrderJpaRepositoryAdapter(SpringDataOrderJpaRepository jpa, OrderPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(jpa.save(mapper.toEntity(order)));
    }
}
