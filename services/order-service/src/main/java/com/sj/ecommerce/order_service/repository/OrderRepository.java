package com.sj.ecommerce.order_service.repository;

import com.sj.ecommerce.order_service.enitity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
