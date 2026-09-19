package com.cloudeflair.fooddelivery.orderservice.repository;

import com.cloudeflair.fooddelivery.orderservice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
