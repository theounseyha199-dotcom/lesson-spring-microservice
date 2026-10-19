package kh.edu.istad.platform.order.domain.port.out;

import kh.edu.istad.platform.order.domain.entity.Order;

public interface OrderRepository {
    Order save(Order order);
}
