package kh.edu.istad.platform.order.application;

import kh.edu.istad.platform.order.domain.Order;

public interface OrderRepository {
    Order save(Order order);
}
