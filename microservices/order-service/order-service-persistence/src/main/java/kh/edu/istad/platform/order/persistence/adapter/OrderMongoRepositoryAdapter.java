package kh.edu.istad.platform.order.persistence.adapter;

import kh.edu.istad.platform.order.domain.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import org.springframework.stereotype.Repository;
import kh.edu.istad.platform.order.persistence.repository.SpringDataOrderMongoRepository;
import kh.edu.istad.platform.order.persistence.mapper.OrderPersistenceMapper;

@Repository
public class OrderMongoRepositoryAdapter implements OrderRepository {
    private final SpringDataOrderMongoRepository mongo;
    private final OrderPersistenceMapper mapper;

    public OrderMongoRepositoryAdapter(SpringDataOrderMongoRepository mongo, OrderPersistenceMapper mapper) {
        this.mongo = mongo;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(mongo.save(mapper.toEntity(order)));
    }
}

